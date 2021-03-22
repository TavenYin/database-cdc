package com.github.taven.xstream;

import oracle.jdbc.OracleConnection;
import oracle.sql.NUMBER;
import oracle.streams.StreamsException;
import oracle.streams.XStreamLCRCallbackHandler;
import oracle.streams.XStreamOut;
import oracle.streams.XStreamUtility;

public class OracleXStreamCDC {
    XStreamLCRCallbackHandler lcrCallback = new LcrEventHandler();

    public void start(OracleConnection connection, String outboundServer, long startScn) {
        XStreamOut xStreamOut = null;
        try {
            xStreamOut = XStreamOut.attach(connection, outboundServer, XStreamUtility.convertSCNToPosition(new NUMBER(startScn)),
                    1, 1, XStreamOut.DEFAULT_MODE);

            while (true)
                xStreamOut.receiveLCRCallback(lcrCallback, XStreamOut.DEFAULT_MODE);

        } catch (StreamsException e) {
            e.printStackTrace();
        }
        finally {
            if (xStreamOut != null) {
                try {
                    xStreamOut.detach(XStreamOut.DEFAULT_MODE);
                } catch (StreamsException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
