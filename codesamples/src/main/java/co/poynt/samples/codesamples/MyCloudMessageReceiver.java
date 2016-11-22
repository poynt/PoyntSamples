package co.poynt.samples.codesamples;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Set;

public class MyCloudMessageReceiver extends BroadcastReceiver {
    public MyCloudMessageReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
//        throw new UnsupportedOperationException("Not yet implemented");
        Set keys = intent.getExtras().keySet();
        for (Object key : keys){
            System.out.println(key + ": " + intent.getExtras().get((String)key));
        }
    }
}
