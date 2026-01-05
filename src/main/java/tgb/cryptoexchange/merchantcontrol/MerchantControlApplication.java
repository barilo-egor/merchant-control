package tgb.cryptoexchange.merchantcontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"tgb.cryptoexchange"})
public class MerchantControlApplication {

	public static void main(String[] args) {
		SpringApplication.run(MerchantControlApplication.class, args);
	}

}
