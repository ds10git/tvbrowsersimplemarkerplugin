/*
 * SimpleMarkerPlugin for TV-Browser for Android
 * Copyright (c) 2014 René Mach (rene@tvbrowser.org)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.tvbrowsersimplemarkerplugin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.Plugin;
import org.tvbrowser.devplugin.PluginManager;
import org.tvbrowser.devplugin.PluginMenu;
import org.tvbrowser.devplugin.Program;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A service class that provides a simple marking functionality for TV-Browser for Android.
 * 
 * @author René Mach
 */
public class SimpleMarkerPlugin extends Service {
  /* The id for the mark PluginMenu */
  private static final int MARK_ACTION = 1;
  /* The id for the unmark PluginMenu */
  private static final int UNMARK_ACTION = 2;
  
  /* The preferences key for the marking set */
  private static final String PREF_MARKINGS = "PREF_MARKINGS";
  
  /* The plugin manager of TV-Browser */
  private PluginManager mPluginManager;
  
  /* The set with the marking ids */
  private Set<String> mMarkingProgramIds;
  
  /**
   * At onBind the Plugin for TV-Browser is loaded.
   */
  @Override
  public IBinder onBind(Intent intent) {
    return getBinder;
  }
  
  @Override
  public boolean onUnbind(Intent intent) {
    /* Don't keep instance of plugin manager*/
    mPluginManager = null;
    
    stopSelf();
    
    return false;
  }
  
  @Override
  public void onDestroy() {
    /* Don't keep instance of plugin manager*/
    mPluginManager = null;
    
    super.onDestroy();
  }
  
  private void save() {
    Editor edit = PreferenceManager.getDefaultSharedPreferences(SimpleMarkerPlugin.this).edit();
    
    edit.putStringSet(PREF_MARKINGS, mMarkingProgramIds);
    edit.commit();
  }

  private Plugin.Stub getBinder = new Plugin.Stub() {
    private long mRemovingProgramId = -1;
    
    @Override
    public void openPreferences(List<Channel> subscribedChannels) throws RemoteException {}
    
    @Override
    public boolean onProgramContextMenuSelected(Program program, PluginMenu pluginMenu) throws RemoteException {
      Log.d("info44", "onProgramContextMenuSelected " + program + " " + program.getId() + " " + pluginMenu);
      
      boolean mark = false;
      String programId = String.valueOf(program.getId());
      Log.d("info44", "programId " + programId);
      if(pluginMenu.getId() == MARK_ACTION) {
        if(!mMarkingProgramIds.contains(programId)) {
          mark = true;
          mMarkingProgramIds.add(programId);
          save();
        }
      }
      else {
        Log.d("info44", "unmark " + mMarkingProgramIds.contains(programId) + " " + mPluginManager);
        if(mMarkingProgramIds.contains(programId)) {
          mRemovingProgramId = program.getId();
          
          if(mPluginManager.unmarkProgram(program)) {
            mMarkingProgramIds.remove(programId);
            save();
          }
          
          mRemovingProgramId = -1;
        }
      }
      
      return mark;
    }
    
    @Override
    public void onDeactivation() throws RemoteException {
      mPluginManager = null;
    }
    
    @Override
    public void onActivation(PluginManager pluginManager) throws RemoteException {
      mPluginManager = pluginManager;
      
      mMarkingProgramIds = PreferenceManager.getDefaultSharedPreferences(SimpleMarkerPlugin.this).getStringSet(PREF_MARKINGS, new HashSet<String>());
    }
    
    @Override
    public boolean isMarked(long programId) throws RemoteException {
      return programId != mRemovingProgramId && mMarkingProgramIds.contains(String.valueOf(programId));
    }
    
    @Override
    public boolean hasPreferences() throws RemoteException {
      return false;
    }
    
    @Override
    public void handleFirstKnownProgramId(long programId) throws RemoteException {
      if(programId == -1) {
        mMarkingProgramIds.clear();
      }
      else {
        String[] knownIds = mMarkingProgramIds.toArray(new String[mMarkingProgramIds.size()]);
        
        for(int i = knownIds.length-1; i >= 0; i--) {
          if(Long.parseLong(knownIds[i]) < programId) {
            mMarkingProgramIds.remove(knownIds[i]);
          }
        }
      }
    }
    
    @Override
    public String getVersion() throws RemoteException {
      return getString(R.string.version);
    }
    
    @Override
    public String getName() throws RemoteException {
      return getString(R.string.service_simplemarker_name);
    }
    
    @Override
    public long[] getMarkedPrograms() throws RemoteException {
      long[] markings = new long[mMarkingProgramIds.size()];
      
      Iterator<String> values = mMarkingProgramIds.iterator();
      
      for(int i = 0; i < markings.length; i++) {
        markings[i] = Long.parseLong(values.next());
      }
      
      return markings;
    }
    
    @Override
    public byte[] getMarkIcon() throws RemoteException {
      return null;
    }
    
    @Override
    public String getLicense() throws RemoteException {
      return getString(R.string.license);
    }
    
    @Override
    public String getDescription() throws RemoteException {
      return getString(R.string.service_simplemarker_description);
    }
    
    @Override
    public PluginMenu[] getContextMenuActionsForProgram(Program program) throws RemoteException {
      PluginMenu menu = null;
      
      if(!mMarkingProgramIds.contains(String.valueOf(program.getId()))) {
        menu = new PluginMenu(MARK_ACTION, getString(R.string.service_simplemarker_context_menu_mark));
      }
      else {
        menu = new PluginMenu(UNMARK_ACTION, getString(R.string.service_simplemarker_context_menu_unmark));
      }
      
      return new PluginMenu[] {menu};
    }
    
    @Override
    public String getAuthor() throws RemoteException {
      return "René Mach";
    }
  };
}
