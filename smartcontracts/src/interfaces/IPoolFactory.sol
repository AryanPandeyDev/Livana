// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/**
 * @title IPoolFactory
 * @notice Interface for the PoolFactory contract, used by FundPool
 *         to verify NGO status at release-time and by LivanaSBT
 *         to verify that the minter is a legitimate pool.
 */
interface IPoolFactory {
    /// @notice Returns true if the given address is a verified NGO.
    function isVerified(address ngo) external view returns (bool);

    /// @notice Returns true if the given address is a pool deployed by this factory.
    function isPool(address pool) external view returns (bool);
}
