package org.topsmoker.cryptobot.config;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
public class Credentials {
    @XmlElement(name = "Phone", required = true)
    private String phone;
    @XmlElement(name = "Password", nillable = true)
    private String password;
}