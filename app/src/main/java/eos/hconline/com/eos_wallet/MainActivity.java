package eos.hconline.com.eos_wallet;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Locale;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author haichecker
 * @date 18-2-28 下午12:13
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private AppCompatEditText myAddress_edt, privateKey_edt, send_edt, recv_edt, num_edt;
    private AppCompatButton createWallet_btn, selectWallet_btn, input_btn, send_btn;
    private AppCompatTextView select_text, status_text;
    private Web3j web3j;
    private Credentials credentials;
    private ProgressDialog progressDialog;
    @SuppressLint("InlinedApi")
    private File keyFile;
    private File eosFile = new File(Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOCUMENTS) + "/eos/");

    private Eosx eosx;


    private void changeEosx() {
        /**
         * 加载合约，通过地址
         */
        eosx = Eosx.load("0x85ce1b8ad25F866783aa7d95F5d77E3b22CDd731", web3j,
                credentials, BigInteger.valueOf(27000000000L), BigInteger.valueOf(250000));
    }


    private void initView() {
        progressDialog = new ProgressDialog(this);

        myAddress_edt = findViewById(R.id.my_address);
        privateKey_edt = findViewById(R.id.private_key);
        send_edt = findViewById(R.id.send);
        recv_edt = findViewById(R.id.revc);
        num_edt = findViewById(R.id.num);


        createWallet_btn = findViewById(R.id.create);
        selectWallet_btn = findViewById(R.id.select);
        input_btn = findViewById(R.id.input);
        send_btn = findViewById(R.id.send_eos);

        createWallet_btn.setOnClickListener(this);
        selectWallet_btn.setOnClickListener(this);
        input_btn.setOnClickListener(this);
        send_btn.setOnClickListener(this);

        select_text = findViewById(R.id.select_text);
        status_text = findViewById(R.id.status);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
        initView();
        //RPC-JSON 初始化
        web3j = Web3jFactory.build(new HttpService("https://rinkeby.infura.io/eosx"));

        //判断密钥文件是否存在，存在则禁用创建按钮
        if (eosFile.exists() && eosFile.listFiles().length > 0) {
            createWallet_btn.setEnabled(false);
            loadCredentials(eosFile.listFiles()[0]);
        } else {
            eosFile.mkdirs();
        }

    }

    /**
     * 通过RxJava加载私玥文件，异步处理
     *
     * @param file 私玥文件
     */
    private void loadCredentials(final File file) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    subscriber.onStart();
                    credentials = WalletUtils.loadCredentials("Shiwenping123", file);
                    if (credentials == null) {
                        subscriber.onError(new Exception("null"));
                        return;
                    }
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    subscriber.onError(e);
                } catch (CipherException e) {
                    subscriber.onError(e);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        progressDialog.setMessage("加载本地钱包中");
                        progressDialog.show();
                    }

                    @Override
                    public void onCompleted() {
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, "本地钱包加载失败！", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        myAddress_edt.setText(credentials.getAddress());
                        changeEosx();
                    }
                });
    }

    /**
     * 查询余额通过RxJava异步
     *
     * @return
     */
    private Observable<BigInteger> balanceOfObs() {
        return eosx.balanceOf(getMyAddress()).observable().observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    private String getMyAddress() {
        return myAddress_edt.getText().toString().trim();
    }

    private String getPrivateKey() {
        return privateKey_edt.getText().toString().trim();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.create:
                create();
                break;
            case R.id.select:
                if (TextUtils.isEmpty(getMyAddress())) {
                    Toast.makeText(this, "请输入钱包地址！", Toast.LENGTH_SHORT).show();
                    return;
                }
                balanceOf();
                break;
            case R.id.input:
                if (TextUtils.isEmpty(getPrivateKey())) {
                    Toast.makeText(this, "请输入私玥！", Toast.LENGTH_SHORT).show();
                    return;
                }
                importFunc();
                break;
            default:
                if (TextUtils.isEmpty(getRecv()) || getNum() <= 0) {
                    Toast.makeText(this, "请输入交易信息！", Toast.LENGTH_SHORT).show();
                    return;
                }
                transfer();
                break;
        }
    }

    private String getRecv() {
        return recv_edt.getText().toString().trim();
    }

    private Double getNum() {
        if (TextUtils.isEmpty(num_edt.getText().toString().trim())) {
            return 0.0;
        }
        return Double.parseDouble(num_edt.getText().toString().trim());
    }

    /**
     * 交易
     */
    private void transfer() {
        transferObs().subscribe(new BaseMySubscriber<TransactionReceipt>() {
            @Override
            public void onNext(TransactionReceipt transactionReceipt) {
                status_text.setText(String.format(Locale.CHINESE, "交易状态:%s", transactionReceipt.getTransactionHash()));
            }
        });
    }

    /**
     * 交易  通过RxJava异步
     *
     * @return
     */
    private Observable<TransactionReceipt> transferObs() {
        return eosx.transfer(getRecv(), BigInteger.valueOf((long) (getNum() * 100))).observable().observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.newThread());
    }

    private void importFunc() {
        importObs()
                .subscribe(new BaseMySubscriber<Credentials>() {
                    @Override
                    public void onNext(Credentials credentials) {
                        if (credentials != null) {
                            MainActivity.this.credentials = credentials;
                            changeEosx();
                            send_edt.setText(credentials.getAddress());
                        }
                    }
                });
    }

    /**
     * 导入私玥，通过RxJava异步导入
     *
     * @return
     */
    private Observable<Credentials> importObs() {
        return Observable.create(new Observable.OnSubscribe<Credentials>() {
            @Override
            public void call(Subscriber<? super Credentials> subscriber) {
                subscriber.onStart();
                try {
                    ECKeyPair private_key = ECKeyPair.create(Numeric.toBigInt(getPrivateKey()));
                    Credentials credentials = Credentials.create(private_key);
                    subscriber.onNext(credentials);
                } catch (Exception e) {
                    subscriber.onError(e);
                } finally {
                    subscriber.onCompleted();
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    /**
     * 创建钱包（密码：Shiwenping123）
     *
     * @return
     */
    private Observable<String> createObs() {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onStart();
                String fileName = null;
                try {
                    fileName = WalletUtils.generateNewWalletFile("Shiwenping123", eosFile, false);
                    keyFile = new File(eosFile, fileName);
                    credentials = WalletUtils.loadCredentials("Shiwenping123", keyFile);
                    subscriber.onNext(fileName);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());
    }

    private void create() {
        createObs()
                .subscribe(new BaseMySubscriber<String>() {
                    @Override
                    public void onNext(String s) {
                        myAddress_edt.setText(credentials.getAddress());
                        changeEosx();
                    }
                });
    }

    private void balanceOf() {
        balanceOfObs()
                .subscribe(new BaseMySubscriber<BigInteger>() {
                    @Override
                    public void onNext(BigInteger bigInteger) {
                        select_text.setText(String.format(Locale.CHINESE, "EOS-X:%.2f", (bigInteger.doubleValue() / 100)));
                    }
                });
    }

    private abstract class BaseMySubscriber<T> extends Subscriber<T> {
        @Override
        public void onStart() {
            super.onStart();
            progressDialog.setMessage("请稍候...");
            progressDialog.show();
        }

        @Override
        public void onCompleted() {
            progressDialog.dismiss();
        }

        @Override
        public void onError(Throwable e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
