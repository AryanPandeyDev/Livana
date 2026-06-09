// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {LivanaSBT} from "./LivanaSBT.sol";
import {FundPool} from "./FundPool.sol";

/**
 * @title PoolFactory
 * @notice Global registry for verified NGOs and deployer of FundPool escrow
 *         contracts. Deploys LivanaSBT in its constructor to resolve the
 *         circular dependency.
 *
 * @dev Access control:
 *   - Only the Safe multi-sig wallet can verify/revoke NGOs
 *   - Only verified NGOs can deploy pools
 *   - Safe wallet address is set via one-time setMultiSigAdmin()
 */
contract PoolFactory {
    // -----------------------------------------------------------------------
    //  Errors
    // -----------------------------------------------------------------------

    error NotMultiSig();
    error NotVerifiedNGO();
    error ZeroAddress();
    error AlreadyVerified();
    error NotCurrentlyVerified();
    error MultiSigAlreadySet();
    error MultiSigNotSet();
    error EmptyCID();

    // -----------------------------------------------------------------------
    //  Events
    // -----------------------------------------------------------------------

    event NGOApproved(address indexed ngo);
    event NGORevoked(address indexed ngo);
    event PoolDeployed(
        address indexed poolAddress,
        address indexed creator,
        uint256 indexed poolIndex,
        string metadataCid
    );
    event MultiSigAdminSet(address indexed multiSigAdmin);

    // -----------------------------------------------------------------------
    //  State
    // -----------------------------------------------------------------------

    address public multiSigAdmin;
    IERC20 public immutable usdc;
    LivanaSBT public immutable sbt;

    mapping(address => bool) public verifiedNGOs;
    mapping(address => bool) public isPool;
    uint256 public poolCount;

    // -----------------------------------------------------------------------
    //  Modifiers
    // -----------------------------------------------------------------------

    modifier onlyMultiSig() {
        if (msg.sender != multiSigAdmin) revert NotMultiSig();
        _;
    }

    modifier onlyVerifiedNGO() {
        if (!verifiedNGOs[msg.sender]) revert NotVerifiedNGO();
        _;
    }

    // -----------------------------------------------------------------------
    //  Constructor
    // -----------------------------------------------------------------------

    /**
     * @param _usdc USDC token contract address.
     * @dev Deploys LivanaSBT internally to resolve the circular dependency.
     *      The Safe multi-sig wallet address must be set separately via setMultiSigAdmin().
     */
    constructor(address _usdc) {
        if (_usdc == address(0)) revert ZeroAddress();
        usdc = IERC20(_usdc);

        // Deploy SBT — the SBT constructor takes this factory's address
        sbt = new LivanaSBT(address(this));
    }

    // -----------------------------------------------------------------------
    //  One-Time Setup
    // -----------------------------------------------------------------------

    /**
     * @notice Set the Safe multi-sig wallet address. Can only be called once.
     * @param _multiSigAdmin The deployed Safe wallet address.
     */
    function setMultiSigAdmin(address _multiSigAdmin) external {
        if (multiSigAdmin != address(0)) revert MultiSigAlreadySet();
        if (_multiSigAdmin == address(0)) revert ZeroAddress();
        multiSigAdmin = _multiSigAdmin;
        emit MultiSigAdminSet(_multiSigAdmin);
    }

    // -----------------------------------------------------------------------
    //  NGO Registry (multi-sig only)
    // -----------------------------------------------------------------------

    /**
     * @notice Whitelist an NGO wallet address. Called via multi-sig after
     *         2-of-3 admin approval of the NGO application.
     * @param _ngo NGO wallet address to verify.
     */
    function addVerifiedNGO(address _ngo) external onlyMultiSig {
        if (_ngo == address(0)) revert ZeroAddress();
        if (verifiedNGOs[_ngo]) revert AlreadyVerified();

        verifiedNGOs[_ngo] = true;
        emit NGOApproved(_ngo);
    }

    /**
     * @notice Remove an NGO from the whitelist. Takes effect immediately
     *         across ALL pools — releaseFunds() checks isVerified() at
     *         release-time.
     * @param _ngo NGO wallet address to revoke.
     */
    function revokeNGO(address _ngo) external onlyMultiSig {
        if (!verifiedNGOs[_ngo]) revert NotCurrentlyVerified();

        verifiedNGOs[_ngo] = false;
        emit NGORevoked(_ngo);
    }

    /**
     * @notice Check if an address is a verified NGO.
     * @param _ngo Address to check.
     * @return True if the address is verified.
     */
    function isVerified(address _ngo) external view returns (bool) {
        return verifiedNGOs[_ngo];
    }

    // -----------------------------------------------------------------------
    //  Pool Deployment (verified NGOs only)
    // -----------------------------------------------------------------------

    /**
     * @notice Deploy a new FundPool escrow contract for the calling NGO.
     * @dev The caller becomes the pool's creator and sole fund recipient.
     *      The 2-of-3 multi-sig approval on each release serves as the
     *      primary safety gate.
     * @param _metadataCid IPFS CID pointing to a JSON blob with pool metadata
     *                     (title, description, region, coverImage, targetAmount).
     * @return poolAddress The address of the newly deployed FundPool.
     */
    function deployPool(string calldata _metadataCid)
        external
        onlyVerifiedNGO
        returns (address poolAddress)
    {
        if (multiSigAdmin == address(0)) revert MultiSigNotSet();
        if (bytes(_metadataCid).length == 0) revert EmptyCID();

        FundPool pool = new FundPool(
            address(usdc),
            address(this),
            multiSigAdmin,
            msg.sender, // creator = the verified NGO
            address(sbt),
            _metadataCid
        );

        poolAddress = address(pool);
        isPool[poolAddress] = true;

        uint256 index = poolCount;
        poolCount++;

        emit PoolDeployed(poolAddress, msg.sender, index, _metadataCid);
    }
}
