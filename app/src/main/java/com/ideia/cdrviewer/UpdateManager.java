package com.ideia.cdrviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UpdateManager {
  private static final String API="https://api.github.com/repos/eltonboni2505/Cdr-viwer/releases/latest";
  private final Activity activity;
  public UpdateManager(Activity activity){this.activity=activity;}
  public void check(boolean manual){
    new Thread(()->{
      try{
        HttpURLConnection conn=(HttpURLConnection)new URL(API).openConnection();
        conn.setConnectTimeout(10000);conn.setReadTimeout(15000);conn.setRequestProperty("Accept","application/vnd.github+json");conn.setRequestProperty("User-Agent","CDRViewer-Android");
        if(conn.getResponseCode()!=200){conn.disconnect();if(manual)notice("Ainda não há uma versão publicada em Releases.");return;}
        JSONObject release;try(InputStream in=conn.getInputStream()){release=new JSONObject(read(in,300000));}finally{conn.disconnect();}
        int remote=0;String tag=release.optString("tag_name","");
        if(tag.startsWith("v"))tag=tag.substring(1);
        String[] nums=tag.split("\\.");
        for(int j=0;j<Math.min(3,nums.length);j++)remote=remote*100+Integer.parseInt(nums[j]);
        int local=0;String[] localNums=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName.split("\\.");
        for(int j=0;j<Math.min(3,localNums.length);j++)local=local*100+Integer.parseInt(localNums[j]);
        if(remote<=local){if(manual)notice("O aplicativo já está atualizado.");return;}
        JSONArray assets=release.optJSONArray("assets");String url=null;
        if(assets!=null)for(int j=0;j<assets.length();j++){JSONObject asset=assets.getJSONObject(j);if(asset.optString("name").toLowerCase().endsWith(".apk")){url=asset.optString("browser_download_url");break;}}
        if(url==null){if(manual)notice("Há uma nova versão, mas ainda não foi publicado um APK.");return;}
        final String apkUrl=url,version=tag;
        activity.runOnUiThread(()->new AlertDialog.Builder(activity).setTitle("Atualização disponível").setMessage("Versão "+version+". Deseja baixar e instalar?").setNegativeButton("Agora não",null).setPositiveButton("Atualizar",(d,w)->download(apkUrl)).show());
      }catch(Exception e){if(manual)notice("Não foi possível verificar atualizações. Confira a conexão.");}
    }).start();
  }
  private void download(String url){notice("Baixando atualização...");new Thread(()->{
    try{
      HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);
      if(c.getResponseCode()!=200)throw new IOException("Download falhou");
      File file=new File(activity.getCacheDir(),"cdr-viewer-update.apk");
      try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(file)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}finally{c.disconnect();}
      activity.runOnUiThread(()->install(file));
    }catch(Exception e){notice("Falha ao baixar a atualização.");}
  }).start();}
  private void install(File file){
    if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
      notice("Autorize instalar apps desta fonte e toque em Atualizar novamente.");
      Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName()));activity.startActivity(settings);return;
    }
    Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".files",file);
    Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(uri,"application/vnd.android.package-archive");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);activity.startActivity(i);
  }
  private void notice(String s){activity.runOnUiThread(()->Toast.makeText(activity,s,Toast.LENGTH_LONG).show());}
  private static String read(InputStream in,int max)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("Resposta grande");out.write(b,0,n);}return out.toString("UTF-8");}
}
