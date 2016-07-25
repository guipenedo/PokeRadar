package com.guipenedo.pokeradar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FilteredNotifierScan extends Service {
    public FilteredNotifierScan() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
