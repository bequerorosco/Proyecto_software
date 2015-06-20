package com.itwizard.Mezzofanti;

import android.test.ActivityInstrumentationTestCase;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.example.HelloJni.HelloJniTest \
 * com.example.HelloJni.tests/android.test.InstrumentationTestRunner
 */
public class MezzofantiTest extends ActivityInstrumentationTestCase<Mezzofanti> {

    public MezzofantiTest() {
        super("com.itwizard.mezzofanti", Mezzofanti.class);
    }

}
