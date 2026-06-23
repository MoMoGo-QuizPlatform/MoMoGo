package org.momogo.momogo;

import org.springframework.boot.SpringApplication;

public class TestMomogoApplication {

	public static void main(String[] args) {
		SpringApplication.from(MomogoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
