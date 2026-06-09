package com.livana.backend.indexer.service;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Static event definitions matching the Solidity contracts.
 * Pre-computes event signature hashes (topic0) for efficient log filtering.
 *
 * All event signatures are derived directly from the Solidity source:
 * - PoolFactory.sol: NGOApproved, NGORevoked, PoolDeployed, MultiSigAdminSet
 * - FundPool.sol: DonationReceived, ProofSubmitted, FundsReleased
 * - LivanaSBT.sol: Locked
 * - Pausable.sol (OZ): Paused, Unpaused
 */
public final class EventDefinitions {

    private EventDefinitions() {} // utility class

    // ========================================================================
    // PoolFactory events
    // ========================================================================

    /** event NGOApproved(address indexed ngo) */
    public static final Event NGO_APPROVED = new Event("NGOApproved",
            List.of(new TypeReference<Address>(true) {}));

    /** event NGORevoked(address indexed ngo) */
    public static final Event NGO_REVOKED = new Event("NGORevoked",
            List.of(new TypeReference<Address>(true) {}));

    /** event PoolDeployed(address indexed poolAddress, address indexed creator, uint256 indexed poolIndex, string metadataCid) */
    public static final Event POOL_DEPLOYED = new Event("PoolDeployed",
            Arrays.asList(
                    new TypeReference<Address>(true) {},   // poolAddress (indexed)
                    new TypeReference<Address>(true) {},   // creator (indexed)
                    new TypeReference<Uint256>(true) {},    // poolIndex (indexed)
                    new TypeReference<Utf8String>(false) {} // metadataCid (non-indexed)
            ));

    /** event MultiSigAdminSet(address indexed multiSigAdmin) */
    public static final Event MULTI_SIG_ADMIN_SET = new Event("MultiSigAdminSet",
            List.of(new TypeReference<Address>(true) {}));

    // ========================================================================
    // FundPool events
    // ========================================================================

    /** event DonationReceived(address indexed donor, uint256 amount) */
    public static final Event DONATION_RECEIVED = new Event("DonationReceived",
            Arrays.asList(
                    new TypeReference<Address>(true) {},  // donor (indexed)
                    new TypeReference<Uint256>(false) {}  // amount (non-indexed)
            ));

    /** event ProofSubmitted(uint256 indexed proofId, string ipfsCid, uint256 amount) */
    public static final Event PROOF_SUBMITTED = new Event("ProofSubmitted",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {},    // proofId (indexed)
                    new TypeReference<Utf8String>(false) {}, // ipfsCid (non-indexed)
                    new TypeReference<Uint256>(false) {}    // amount (non-indexed)
            ));

    /** event FundsReleased(uint256 indexed proofId, address indexed ngo, uint256 amount) */
    public static final Event FUNDS_RELEASED = new Event("FundsReleased",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {},  // proofId (indexed)
                    new TypeReference<Address>(true) {},  // ngo (indexed)
                    new TypeReference<Uint256>(false) {}  // amount (non-indexed)
            ));

    // ========================================================================
    // LivanaSBT events
    // ========================================================================

    /** event Locked(uint256 tokenId) — EIP-5192, non-indexed */
    public static final Event LOCKED = new Event("Locked",
            List.of(new TypeReference<Uint256>(false) {}));

    // ========================================================================
    // Pausable (OpenZeppelin) events
    // ========================================================================

    /** event Paused(address account) — non-indexed */
    public static final Event PAUSED = new Event("Paused",
            List.of(new TypeReference<Address>(false) {}));

    /** event Unpaused(address account) — non-indexed */
    public static final Event UNPAUSED = new Event("Unpaused",
            List.of(new TypeReference<Address>(false) {}));

    // ========================================================================
    // Pre-computed event signature hashes (topic0)
    // ========================================================================

    public static final String NGO_APPROVED_SIG = EventEncoder.encode(NGO_APPROVED);
    public static final String NGO_REVOKED_SIG = EventEncoder.encode(NGO_REVOKED);
    public static final String POOL_DEPLOYED_SIG = EventEncoder.encode(POOL_DEPLOYED);
    public static final String MULTI_SIG_ADMIN_SET_SIG = EventEncoder.encode(MULTI_SIG_ADMIN_SET);
    public static final String DONATION_RECEIVED_SIG = EventEncoder.encode(DONATION_RECEIVED);
    public static final String PROOF_SUBMITTED_SIG = EventEncoder.encode(PROOF_SUBMITTED);
    public static final String FUNDS_RELEASED_SIG = EventEncoder.encode(FUNDS_RELEASED);
    public static final String LOCKED_SIG = EventEncoder.encode(LOCKED);
    public static final String PAUSED_SIG = EventEncoder.encode(PAUSED);
    public static final String UNPAUSED_SIG = EventEncoder.encode(UNPAUSED);

    /**
     * Maps event signature (topic0) to event type string for indexed_events table.
     */
    public static final Map<String, String> SIGNATURE_TO_EVENT_TYPE = Map.of(
            NGO_APPROVED_SIG, "NGO_APPROVED",
            NGO_REVOKED_SIG, "NGO_REVOKED",
            POOL_DEPLOYED_SIG, "POOL_DEPLOYED",
            MULTI_SIG_ADMIN_SET_SIG, "MULTI_SIG_ADMIN_SET",
            DONATION_RECEIVED_SIG, "DONATION_RECEIVED",
            PROOF_SUBMITTED_SIG, "PROOF_SUBMITTED",
            FUNDS_RELEASED_SIG, "FUNDS_RELEASED",
            LOCKED_SIG, "SBT_LOCKED",
            PAUSED_SIG, "PAUSED",
            UNPAUSED_SIG, "UNPAUSED"
    );
}
