package ch.software_atelier.simpleflex.rest.auth;

import ch.software_atelier.simpleflex.rest.auth.rres.*;
import ch.software_atelier.simpleflex.rest.auth.rres.test.AuthenticationPageResource;
import ch.software_atelier.simpleflex.rest.auth.rres.test.FailedResource;
import ch.software_atelier.simpleflex.rest.auth.rres.test.SecuredResource;
import ch.software_atelier.simpleflex.rest.auth.rres.test.SuccessResource;
import ch.software_atelier.simpleflex.rest.auth.utils.StrHlp;
import ch.software_atelier.simpleflex.rest.auth.data.DataHandler;
import ch.software_atelier.simpleflex.rest.auth.data.MongoDBDataHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenHandler;
import ch.software_atelier.simpleflex.rest.auth.token.TokenParser;
import ch.software_atelier.simpleflex.SimpleFlexAccesser;
import ch.software_atelier.simpleflex.SimpleFlexBase;
import ch.software_atelier.simpleflex.rest.RestApp;

import java.util.HashMap;

public class App extends RestApp {

    public static void main(String[] args)throws Exception{
        HashMap <String,Object> config = new HashMap<>();
        config.put("$mongoUri", "mongodb://user:*******@software-atelier.ch:27017/db");
        config.put("$secret", "***");
        config.put("$sessionTimeout", "300");
        SimpleFlexBase.serveOnLocalhost(App.class.getName(), config, 18001);
    }
    
    
    @Override
    public void start(String name, HashMap<String,Object> config, SimpleFlexAccesser sfa) {
        // realm entspricht der collection
        
        // es gibt eine App Instanz pro Datenbank.
        
        super.start(name, config, sfa);
        try{
            DataHandler dh = new MongoDBDataHandler(config.get("$mongoUri").toString());
            TokenHandler th = new TokenHandler(
                    config.get("$secret").toString(),
                    dh,
                    (int) StrHlp.parseLong(config.get("$sessionTimeout").toString())
            );
            TokenParser tp = new TokenParser(config.get("$secret").toString());
            // Login & Session renewal
            addResource("/session", new SessionResource(dh,th,tp));
            // User creation & Listing & updating
            addResource("/user", new UserResource(dh,th,tp));
            // User Details & Update
            addResource("/user/{name}", new SpecificUserResource(dh,th,tp));
            // User Settings get & set
            addResource("/user/{name}/settings", new UserSettingsResource(dh,th,tp));
            // Cookie Authentication
            addResource("/login", new WebsiteLoginResource(dh,th,"/success","/failed",60*60));

            // Test Implementation
            addResource("/auth", new AuthenticationPageResource());
            addResource("/failed", new FailedResource());
            addResource("/success", new SuccessResource());
            addResource("/secured", new SecuredResource(tp));
        }catch(Throwable uhe){
            uhe.printStackTrace();
        }
        
    }
    
    
    
}
