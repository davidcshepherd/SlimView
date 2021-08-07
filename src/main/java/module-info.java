module io.mozib.slimview {
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires java.base;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.swing;
    requires imgscalr.lib;
    requires org.apache.commons.io;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.dataformat.xml;
    requires java.prefs;
    requires metadata.extractor;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;

    opens io.mozib.slimview to javafx.fxml;
    exports io.mozib.slimview;
}
