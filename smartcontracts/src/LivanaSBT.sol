// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {ERC721} from "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import {IERC165} from "@openzeppelin/contracts/utils/introspection/IERC165.sol";
import {IPoolFactory} from "./interfaces/IPoolFactory.sol";
import {ILivanaSBT} from "./interfaces/ILivanaSBT.sol";

/**
 * @title LivanaSBT
 * @notice Soulbound Reputation Token for Livana. An ERC-721 NFT that cannot
 *         be transferred between wallets. Minted to an NGO's address after
 *         every successful fund release, forming a permanent, on-chain
 *         reputation history.
 *
 * @dev EIP-5192 compliant:
 *   - locked(tokenId) always returns true (all tokens permanently locked)
 *   - supportsInterface returns true for 0xb45a3c0e (EIP-5192 interface ID)
 *   - Emits Locked(tokenId) on every mint
 *
 * Security:
 *   - Only FundPool contracts (verified via PoolFactory.isPool) can mint
 *   - Transfers between non-zero addresses are blocked via _update override
 *   - Burns are also blocked — SBTs are permanent
 */
contract LivanaSBT is ERC721, ILivanaSBT {
    // -----------------------------------------------------------------------
    //  Errors
    // -----------------------------------------------------------------------

    error NotAPool();
    error TransferBlocked();
    error TokenDoesNotExist();

    // -----------------------------------------------------------------------
    //  Events (EIP-5192)
    // -----------------------------------------------------------------------

    /// @notice Emitted when a token is locked (on every mint). EIP-5192.
    event Locked(uint256 tokenId);

    // -----------------------------------------------------------------------
    //  State
    // -----------------------------------------------------------------------

    /// @notice PoolFactory address — used to verify minter is a legitimate pool.
    IPoolFactory public immutable factory;

    /// @notice Total SBTs minted (also used as tokenId counter).
    uint256 public tokenCount;

    /// @notice On-chain reputation data for each SBT.
    struct ReputationData {
        address pool;       // FundPool contract that triggered this mint
        uint256 amount;     // USDC amount (6 decimals) that was verified and released
        uint256 timestamp;  // Block timestamp of minting
    }

    /// @notice tokenId => reputation data
    mapping(uint256 => ReputationData) public reputation;

    // -----------------------------------------------------------------------
    //  Modifiers
    // -----------------------------------------------------------------------

    modifier onlyPool() {
        if (!factory.isPool(msg.sender)) revert NotAPool();
        _;
    }

    // -----------------------------------------------------------------------
    //  Constructor
    // -----------------------------------------------------------------------

    /**
     * @param _factory PoolFactory contract address.
     */
    constructor(address _factory)
        ERC721("Livana Reputation", "LVNA")
    {
        factory = IPoolFactory(_factory);
    }

    // -----------------------------------------------------------------------
    //  Minting (only callable by FundPool contracts)
    // -----------------------------------------------------------------------

    /**
     * @notice Mint a new SBT to an NGO after a successful fund release.
     * @dev Only callable by addresses registered as pools in PoolFactory.
     *      Uses _mint instead of _safeMint to avoid reentrancy vector via
     *      onERC721Received callback. The NGO is a known verified wallet,
     *      not an arbitrary contract.
     * @param _ngo    NGO wallet address to receive the SBT.
     * @param _amount USDC amount (6 decimals) that was verified and released.
     * @return tokenId The ID of the newly minted SBT.
     */
    function mint(address _ngo, uint256 _amount)
        external
        onlyPool
        returns (uint256 tokenId)
    {
        tokenId = tokenCount;
        tokenCount++;

        reputation[tokenId] = ReputationData({
            pool: msg.sender,
            amount: _amount,
            timestamp: block.timestamp
        });

        _mint(_ngo, tokenId);

        emit Locked(tokenId);
    }

    // -----------------------------------------------------------------------
    //  EIP-5192: Soulbound Interface
    // -----------------------------------------------------------------------

    /**
     * @notice Returns whether a token is locked (non-transferable).
     * @dev Always returns true — all Livana SBTs are permanently locked.
     * @param tokenId Token ID to check.
     * @return True (always).
     */
    function locked(uint256 tokenId) external view returns (bool) {
        if (_ownerOf(tokenId) == address(0)) revert TokenDoesNotExist();
        return true;
    }

    /**
     * @notice EIP-165 interface detection.
     * @dev Returns true for EIP-5192 interface ID (0xb45a3c0e) in addition
     *      to standard ERC-721 interfaces.
     */
    function supportsInterface(bytes4 interfaceId)
        public
        view
        override
        returns (bool)
    {
        return
            interfaceId == 0xb45a3c0e || // EIP-5192
            super.supportsInterface(interfaceId);
    }

    // -----------------------------------------------------------------------
    //  View Functions
    // -----------------------------------------------------------------------

    /**
     * @notice Get the full reputation data for a token.
     * @dev Use ownerOf(tokenId) to get the NGO address.
     * @param tokenId Token ID to query.
     */
    function getReputation(uint256 tokenId)
        external
        view
        returns (
            address pool,
            uint256 amount,
            uint256 timestamp
        )
    {
        if (_ownerOf(tokenId) == address(0)) revert TokenDoesNotExist();
        ReputationData storage r = reputation[tokenId];
        return (r.pool, r.amount, r.timestamp);
    }

    // -----------------------------------------------------------------------
    //  Transfer Blocking (Soulbound Enforcement)
    // -----------------------------------------------------------------------

    /**
     * @dev Override the OZ v5 _update hook to block all transfers between
     *      non-zero addresses. Only minting (from == address(0)) is allowed.
     *      Burns are also blocked (SBTs are permanent).
     */
    function _update(address to, uint256 tokenId, address auth)
        internal
        override
        returns (address)
    {
        address from = _ownerOf(tokenId);

        // Allow minting (from == address(0))
        // Block all other operations (transfers and burns)
        if (from != address(0)) {
            revert TransferBlocked();
        }

        return super._update(to, tokenId, auth);
    }
}
