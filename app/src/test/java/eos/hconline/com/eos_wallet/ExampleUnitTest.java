package eos.hconline.com.eos_wallet;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import rx.functions.Action1;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void create() throws Exception {
        Web3j web3j = Web3jFactory.build(new HttpService("https://rinkeby.infura.io/eosx"));
//        String fileName = WalletUtils.generateNewWalletFile("Shiwenping123", new File("."), false);
        Credentials c = WalletUtils.loadCredentials("Shiwenping123",new File(".","1.json"));
        System.out.println(c.getEcKeyPair().getPublicKey().toString());

//        EthGetBalance ethGetBalance = web3j.ethGetBalance("0xb331beFa33FFfa98fD47781aC3386D78DcEbE80d", DefaultBlockParameterName.LATEST)
//                .sendAsync().get();
//        System.out.print(ethGetBalance.getBalance().toString());
//        assertEquals(4, 2 + 2);

    }

    @Test
    public void balanceOf() throws IOException, CipherException, ExecutionException, InterruptedException {
        Web3j web3j = Web3jFactory.build(new HttpService("https://rinkeby.infura.io/eosx"));
        Credentials credentials = WalletUtils.loadCredentials("Shiwenping123", new File("1.json"));
        Eosx eosx = new Eosx("0x85ce1b8ad25F866783aa7d95F5d77E3b22CDd731", web3j, credentials, BigInteger.valueOf(27000000000L), BigInteger.valueOf(250000));
        Future<BigInteger> f = eosx.balanceOf("0x2d7d2175928a4a0036Cd4e52c94741882891ac91")
                .sendAsync();
        System.out.print(f.get().toString());
    }

    @Test
    public void transfer() throws IOException, CipherException, ExecutionException, InterruptedException {
        Eosx eosx = getEos();
        Future<TransactionReceipt> f = eosx.transfer("0xb331beFa33FFfa98fD47781aC3386D78DcEbE80d", BigInteger.valueOf(50)).sendAsync();
        System.out.print(f.get().getTransactionHash());
    }

    private Eosx getEos() throws IOException, CipherException {
        Web3j web3j = Web3jFactory.build(new HttpService("https://rinkeby.infura.io/eosx"));
        Credentials credentials = WalletUtils.loadCredentials("Shiwenping123", new File("1.json"));
        return new Eosx("0x85ce1b8ad25F866783aa7d95F5d77E3b22CDd731", web3j, credentials, BigInteger.valueOf(27000000000L), BigInteger.valueOf(250000));
    }

    @Test
    public void privateKey() {
        ECKeyPair private_key = ECKeyPair.create(Numeric.toBigInt("ad2a58496b6739170f3415efdecfcd13ee893e84b97a10ea6626299fa0bfd50c"));
        Credentials credentials = Credentials.create(private_key);
        System.out.println(Numeric.encodeQuantity(credentials.getEcKeyPair().getPrivateKey()));
        System.out.print(credentials.getAddress());
    }

    @Test
    public void balanceOfTo()
    {
        Web3j web3j = Web3jFactory.build(new HttpService("https://rinkeby.infura.io/eosx"));
//        web3j.ethGetBalance("0x2d7d2175928a4a0036Cd4e52c94741882891ac91",DefaultBlockParameterName.LATEST)
//                .observable()
//                .subscribe(new Action1<EthGetBalance>() {
//                    @Override
//                    public void call(EthGetBalance ethGetBalance) {
//                        System.out.print(ethGetBalance.getBalance());
//                    }
//                });



        final Function function = new Function("balanceOf",
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address("0x2d7d2175928a4a0036Cd4e52c94741882891ac91")),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        String encodedFunction = FunctionEncoder.encode(function);
        Request<?, EthCall> ethCall = web3j.ethCall(Transaction.createEthCallTransaction("0x2d7d2175928a4a0036Cd4e52c94741882891ac91","0x85ce1b8ad25F866783aa7d95F5d77E3b22CDd731",encodedFunction),DefaultBlockParameterName.LATEST);
        ethCall.observable()
                .subscribe(new Action1<EthCall>() {
                    @Override
                    public void call(EthCall ethCall) {
                        List<Type> a = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
                        for (Type type : a) {
                            System.out.println(type.getTypeAsString());
                        }
                        System.out.print(Numeric.decodeQuantity(ethCall.getValue()));
                    }
                });
    }
}