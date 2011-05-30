package dlna.player.activities;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import dlna.player.R;

public class MainActivity extends TabActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TabHost tabHost = getTabHost();
	    TabHost.TabSpec spec;
	    Intent intent;

	    intent = new Intent().setClass(this, NetworkActivity.class);
	    
	    spec = tabHost.newTabSpec("network").setIndicator("Network",null)
            .setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, ContentActivity.class);
	    
	    spec = tabHost.newTabSpec("content").setIndicator("Content",null)
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, PlaylistActivity.class);
	    spec = tabHost.newTabSpec("playlist").setIndicator("Playlist",null)
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    tabHost.setCurrentTabByTag("network");
    }
}