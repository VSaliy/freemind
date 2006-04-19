/*
 * Created on 12.07.2005
 * Copyright (C) 2005 Dimitri Polivaev
 */
package freemind.main;

import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JFrame;

/**
 * @author Dimitri Polivaev
 * 12.07.2005
 */
public class Resources {
    private FreeMindMain frame;
    static Resources resourcesInstance = null;
    private HashMap countryMap;
    private Resources(FreeMindMain frame) {
        this.frame = frame;  
    }
    
    static public void createInstance(FreeMindMain frame){
        if (resourcesInstance == null) resourcesInstance = new Resources(frame);
    }
    
    public URL getResource(String resource) {
        return getFrame().getResource(resource);
    }
    
    public String getResourceString(String resource) {
        return frame.getResourceString(resource);
    }
         
    static public Resources getInstance(){
        return resourcesInstance;
    }

    public String getFreemindDirectory() {
        return frame.getFreemindDirectory();
    }

    public String getFreemindVersion() {
        return frame.getFreemindVersion();
    }

    public int getIntProperty(String key, int defaultValue) {
        return frame.getIntProperty(key, defaultValue);
    }

    public Properties getProperties() {
        return frame.getProperties();
    }

    public String getProperty(String key) {
        return frame.getProperty(key);
    }

    public ResourceBundle getResources() {
        return frame.getResources();
    }

    public JFrame getJFrame() {
        FreeMindMain f = getFrame();
        if (f instanceof JFrame) return (JFrame) f;
        return null;
    }
    
    public FreeMindMain getFrame() {
        return frame;
    }

    public HashMap getCountryMap() {
        if(countryMap == null){
            String[] countryMapArray = new String[]{ 
                    "de", "DE", "en", "UK", "en", "US", "es", "ES", "es", "MX", "fi", "FI", "fr", "FR", "hu", "HU", "it", "CH",
                    "it", "IT", "nl", "NL", "no", "NO", "pt", "PT", "ru", "RU", "sl", "SI", "uk", "UA", "zh", "CN" };
            
            countryMap = new HashMap();
            for (int i = 0; i < countryMapArray.length; i = i + 2) {
                countryMap.put(countryMapArray[i],countryMapArray[i+1]); }
        }
        return countryMap;
    }
}