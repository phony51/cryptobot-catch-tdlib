package org.topsmoker.cryptobot.config;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.IOException;
import java.io.InputStream;

public class ResourceLoader implements Loader {
    @Override
    public Config load() {
        try (InputStream is = ResourceLoader.class.getResourceAsStream("/config.xml")) {
            JAXBContext context = JAXBContext.newInstance(Config.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Config) unmarshaller.unmarshal(is);
        } catch (IOException | JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
