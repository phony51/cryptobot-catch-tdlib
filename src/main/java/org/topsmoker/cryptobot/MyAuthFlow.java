package org.topsmoker.cryptobot;

import org.topsmoker.cryptobot.utils.ClientAuth;

import java.util.Scanner;

public record MyAuthFlow(String getPhoneNumber, String getPassword) implements ClientAuth.AuthFlow {

    @Override
    public String getCode() {
        System.out.println("Enter a code: ");
        try (Scanner reader = new Scanner(System.in)) {
            return reader.next();
        }
    }

    @Override
    public void onReady() {
        System.out.println("Successfully authorized");
    }
}