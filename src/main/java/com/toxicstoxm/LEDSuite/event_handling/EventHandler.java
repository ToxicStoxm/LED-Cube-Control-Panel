package com.toxicstoxm.LEDSuite.event_handling;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    // annotation for EventHandler
    // every listener method is required to use this annotation
}
