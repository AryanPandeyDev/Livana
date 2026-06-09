// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {SafeERC20} from "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";
import {ReentrancyGuard} from "@openzeppelin/contracts/utils/ReentrancyGuard.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";
import {IPoolFactory} from "./interfaces/IPoolFactory.sol";
import {ILivanaSBT} from "./interfaces/ILivanaSBT.sol";

/**
 * @title FundPool
 * @notice Per-cause escrow contract for Livana. Holds donated USDC, tracks
 *         IPFS proof submissions, and releases funds to the pool's creator NGO
 *         after multi-sig admin approval.
 *
 * @dev Key security properties:
 *   - Funds can only leave via releaseFunds() to the verified creator NGO
 *   - Every release requires 2-of-3 multi-sig approval
 *   - No withdraw(), no closePool(), no selfdestruct, no delegatecall
 *   - No receive() or fallback() — cannot receive ETH
 *   - ReentrancyGuard on fund-moving functions
 *   - Pausable for donation emergency stop
 *   - SafeERC20 for all token interactions
 *   - Checks-Effects-Interactions pattern
 */
contract FundPool is ReentrancyGuard, Pausable {
    using SafeERC20 for IERC20;

    // -----------------------------------------------------------------------
    //  Errors
    // -----------------------------------------------------------------------

    error NotMultiSig();
    error NotCreator();
    error ZeroAmount();
    error ProofDoesNotExist();
    error ProofAlreadyReleased();
    error InsufficientPoolBalance(uint256 requested, uint256 available);
    error NGONotVerified();
    error EmptyCID();
    error ZeroAddress();

    // -----------------------------------------------------------------------
    //  Events
    // -----------------------------------------------------------------------

    event DonationReceived(address indexed donor, uint256 amount);
    event ProofSubmitted(uint256 indexed proofId, string ipfsCid, uint256 amount);
    event FundsReleased(uint256 indexed proofId, address indexed ngo, uint256 amount);

    // -----------------------------------------------------------------------
    //  Immutable State (set in constructor)
    // -----------------------------------------------------------------------

    IERC20 public immutable usdc;
    IPoolFactory public immutable factory;
    address public immutable multiSigAdmin;
    address public immutable creator;
    ILivanaSBT public immutable sbt;

    /// @notice IPFS CID of pool metadata JSON (title, description, region, etc.)
    string public metadataCid;

    // -----------------------------------------------------------------------
    //  Mutable State
    // -----------------------------------------------------------------------

    uint256 public totalDonated;
    uint256 public totalReleased;

    /// @notice Proof submission data
    struct ProofSubmission {
        string ipfsCid;
        uint256 amount;
        uint256 timestamp;
        bool released;
    }

    uint256 public proofCount;
    mapping(uint256 => ProofSubmission) public proofs;

    // -----------------------------------------------------------------------
    //  Modifiers
    // -----------------------------------------------------------------------

    modifier onlyMultiSig() {
        if (msg.sender != multiSigAdmin) revert NotMultiSig();
        _;
    }

    modifier onlyCreator() {
        if (msg.sender != creator) revert NotCreator();
        _;
    }

    // -----------------------------------------------------------------------
    //  Constructor
    // -----------------------------------------------------------------------

    /**
     * @param _usdc          USDC token contract address
     * @param _factory       PoolFactory contract address
     * @param _multiSigAdmin Safe multi-sig wallet address (e.g. Gnosis Safe)
     * @param _creator       NGO wallet that created this pool
     * @param _sbt           LivanaSBT contract address
     * @param _metadataCid   IPFS CID of pool metadata JSON
     */
    constructor(
        address _usdc,
        address _factory,
        address _multiSigAdmin,
        address _creator,
        address _sbt,
        string memory _metadataCid
    ) {
        if (_usdc == address(0)) revert ZeroAddress();
        if (_factory == address(0)) revert ZeroAddress();
        if (_multiSigAdmin == address(0)) revert ZeroAddress();
        if (_creator == address(0)) revert ZeroAddress();
        if (_sbt == address(0)) revert ZeroAddress();

        usdc = IERC20(_usdc);
        factory = IPoolFactory(_factory);
        multiSigAdmin = _multiSigAdmin;
        creator = _creator;
        sbt = ILivanaSBT(_sbt);
        metadataCid = _metadataCid;
    }

    // -----------------------------------------------------------------------
    //  Donation
    // -----------------------------------------------------------------------

    /**
     * @notice Donate USDC to this pool. Requires prior usdc.approve().
     * @param _amount Amount of USDC (6 decimals) to donate.
     */
    function donate(uint256 _amount) external nonReentrant whenNotPaused {
        if (_amount == 0) revert ZeroAmount();

        totalDonated += _amount;
        usdc.safeTransferFrom(msg.sender, address(this), _amount);

        emit DonationReceived(msg.sender, _amount);
    }

    // -----------------------------------------------------------------------
    //  Proof Submission
    // -----------------------------------------------------------------------

    /**
     * @notice Submit an IPFS CID and claimed amount as proof of expenses.
     * @param _ipfsCid IPFS folder CID containing proof documents.
     * @param _amount  Claimed reimbursement amount (USDC, 6 decimals).
     * @return proofId The ID of the newly created proof submission.
     */
    function submitProof(string calldata _ipfsCid, uint256 _amount)
        external
        onlyCreator
        returns (uint256 proofId)
    {
        if (bytes(_ipfsCid).length == 0) revert EmptyCID();
        if (_amount == 0) revert ZeroAmount();

        proofId = proofCount;
        proofCount++;

        proofs[proofId] = ProofSubmission({
            ipfsCid: _ipfsCid,
            amount: _amount,
            timestamp: block.timestamp,
            released: false
        });

        emit ProofSubmitted(proofId, _ipfsCid, _amount);
    }

    // -----------------------------------------------------------------------
    //  Fund Release (multi-sig only)
    // -----------------------------------------------------------------------

    /**
     * @notice Release USDC for a specific proof after multi-sig approval.
     * @dev Follows Checks-Effects-Interactions pattern strictly.
     *      The 2-of-3 multi-sig approval is the primary safety gate —
     *      admins review IPFS proof docs before approving each release.
     *      Mints SBT to creator after successful transfer.
     * @param _proofId Proof submission ID to release funds for.
     */
    function releaseFunds(uint256 _proofId) external onlyMultiSig nonReentrant {
        // --- CHECKS ---
        if (!factory.isVerified(creator)) revert NGONotVerified();
        if (_proofId >= proofCount) revert ProofDoesNotExist();

        ProofSubmission storage proof = proofs[_proofId];
        if (proof.released) revert ProofAlreadyReleased();

        uint256 amount = proof.amount;

        uint256 balance = usdc.balanceOf(address(this));
        if (amount > balance) revert InsufficientPoolBalance(amount, balance);

        // --- EFFECTS ---
        proof.released = true;
        totalReleased += amount;

        // --- INTERACTIONS ---
        usdc.safeTransfer(creator, amount);

        // slither-disable-next-line unused-return
        sbt.mint(creator, amount);

        emit FundsReleased(_proofId, creator, amount);
    }

    // -----------------------------------------------------------------------
    //  Pause Controls (multi-sig only)
    // -----------------------------------------------------------------------

    /**
     * @notice Pause new donations. Does not affect proof submission or releases.
     */
    function pauseDonations() external onlyMultiSig {
        _pause();
    }

    /**
     * @notice Resume donations.
     */
    function resumeDonations() external onlyMultiSig {
        _unpause();
    }

    // -----------------------------------------------------------------------
    //  View Functions
    // -----------------------------------------------------------------------

    /**
     * @notice Get the current USDC balance held by this pool.
     */
    function getPoolBalance() external view returns (uint256) {
        return usdc.balanceOf(address(this));
    }

    /**
     * @notice Get full proof submission details.
     * @param _proofId Proof ID to query.
     */
    function getProof(uint256 _proofId)
        external
        view
        returns (
            string memory ipfsCid,
            uint256 amount,
            uint256 timestamp,
            bool released
        )
    {
        if (_proofId >= proofCount) revert ProofDoesNotExist();
        ProofSubmission storage p = proofs[_proofId];
        return (p.ipfsCid, p.amount, p.timestamp, p.released);
    }
}
