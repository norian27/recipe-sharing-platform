package com.koh0118;

import java.util.Locale;
import java.util.ResourceBundle;

public class LocaleManager {
    private static LocaleManager instance;
    private ResourceBundle resourceBundle;
    private Locale locale;

    private LocaleManager() {
        locale = new Locale("cs", "CZ");
        resourceBundle = ResourceBundle.getBundle("messages", locale);
    }

    public static LocaleManager getInstance() {
        if (instance == null) {
            instance = new LocaleManager();
        }
        return instance;
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        resourceBundle = ResourceBundle.getBundle("messages", this.locale);
    }

    public Locale getLocale() {
        return locale;
    }
}

