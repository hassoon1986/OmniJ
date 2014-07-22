package org.mastercoin.exodus

import org.mastercoin.BaseMainNetSpec
import org.mastercoin.CurrencyID
import org.mastercoin.MPNetworkParameters
import org.mastercoin.MPRegTestParams
import spock.lang.Shared
import spock.lang.Stepwise

import java.lang.Void as Should

/**
 *
 * Test Specification for Initial Exodus Fundraiser
 *
 * This Spec runs @Stepwise which means each test builds on the previous test
 * and the Spec will be aborted upon the first test that fails.
 *
 * User: sean
 * Date: 7/22/14
 * Time: 9:02 AM
 */
@Stepwise
class ExodusFundraiserSpec extends BaseMainNetSpec {
    @Shared
    MPNetworkParameters mpNetParams
    @Shared
    Integer    startHeight
    @Shared
    BigDecimal fundraiserAmountBTC
    @Shared
    BigDecimal extraBTCForTxFees
    @Shared
    String participatingAddress

    void setupSpec() {
        mpNetParams = MPRegTestParams.get()
        fundraiserAmountBTC = 5000.0
        extraBTCForTxFees = 1.0
        // We can start this test 1 blocks before the exodus
        startHeight = mpNetParams.firstExodusBlock - 1
    }

    Should "Be at a block height before Exodus period starts"() {
        when:
        def curHeight = client.getBlockCount()
        println "Current blockheight: ${curHeight}"

        then:
        curHeight <= startHeight

        and: "the Exodus address should have a zero balance"
        0.0 == client.getReceivedByAddress(mpNetParams.exodusAddress, 1)

    }

    Should "Generate blocks to just before Exodus start"() {

        when: "we tell Master Core to mine enough blocks to bring us just before Exodus"
        def curHeight = client.getBlockCount()
        client.setGenerate(true, startHeight-curHeight)

        then: "we are at the expected block"
        startHeight == client.getBlockCount()

    }

    Should "Fund an address with BTC for sending BTC to Exodus address"() {
        when: "we create a new address and send a some mined coins to it"
        participatingAddress = client.getNewAddress()    // Create new Bitcoin/Mastercoin address
        client.sendToAddress(participatingAddress, fundraiserAmountBTC+extraBTCForTxFees,
                "Put some mined coins into an address for the fundraiser", "Initial Mastercoin address")
        client.setGenerate(true, 1)                     // Generate 1 block
        def curHeight = client.getBlockCount()

        then: "the new address has the correct balance in BTC"
        fundraiserAmountBTC+extraBTCForTxFees == client.getReceivedByAddress(participatingAddress, 1)

        and: "we've entered the fundraiser period"
        curHeight == startHeight + 1
        curHeight >= mpNetParams.firstExodusBlock
        curHeight <= mpNetParams.lastExodusBlock
    }

    Should "Buy some Mastercoins by sending BTC to Exodus address"() {
        when: "we send coins to the Exodus address"
        // TODO: #1 Use an RPC method that allows us to specify participatingAddress as sending address
        // TODO: #2 We need to somehow set participatingAddress as the change address
        // TODO: #3 Account for Early Bird bonus that we should receive as mscBalance
        // TODO: #4 Ensure we're getting at least one time quantum (second?) between blocks
        client.sendToAddress(mpNetParams.exodusAddress, fundraiserAmountBTC,
                "Buy some MSC", "Exodus address")
        def blocksToWrite = mpNetParams.postExodusBlock - mpNetParams.firstExodusBlock
        client.setGenerate(true, blocksToWrite)          // Close the fundraiser
        def mscBalance = client.getbalance_MP(participatingAddress, CurrencyID.MSC_VALUE)

        then: "we are at the 'Post Exodus' Block"
        mpNetParams.postExodusBlock == client.getBlockCount()

        and: "the Exodus address has the correct balance"
        fundraiserAmountBTC == client.getReceivedByAddress(mpNetParams.exodusAddress, 1)

        and: "our BTC/MSC address money leftover for Tx fees"
        extraBTCForTxFees == client.getReceivedByAddress(participatingAddress, 1)

        and: "our BTC/MSC address has the correct amount MSC"
        mscBalance >= 100 * fundraiserAmountBTC // need calculation for proper amount
    }
}