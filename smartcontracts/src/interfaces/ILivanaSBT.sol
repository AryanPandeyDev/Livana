// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/**
 * @title ILivanaSBT
 * @notice Interface for the LivanaSBT contract, used by FundPool
 *         to mint reputation tokens after successful fund releases.
 */
interface ILivanaSBT {
    /// @notice Mint a new SBT to an NGO after a successful fund release.
    /// @param ngo    NGO wallet address to receive the SBT.
    /// @param amount USDC amount (6 decimals) that was verified and released.
    /// @return tokenId The ID of the newly minted SBT.
    function mint(address ngo, uint256 amount) external returns (uint256 tokenId);
}
