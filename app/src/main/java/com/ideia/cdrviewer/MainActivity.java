package com.ideia.cdrviewer;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.Locale;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import androidx.core.content.FileProvider;

public class MainActivity extends Activity {
  private ImageView preview; private TextView status; private Bitmap bitmap;
  @Override public void onCreate(Bundle b) { super.onCreate(b);
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,30,20,20);
    TextView title=new TextView(this);title.setText("CDR Viewer · Offline");title.setTextSize(23);root.addView(title);
    status=new TextView(this);status.setText("Abra um CDR para visualizar a prévia incorporada e compartilhar em PNG. A prévia pode ter baixa resolução.");root.addView(status);
    Button open=new Button(this);open.setText("Abrir arquivo");root.addView(open);
    Button share=new Button(this);share.setText("Compartilhar PNG no WhatsApp");root.addView(share);
    preview=new ImageView(this);preview.setAdjustViewBounds(true);root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);});
    share.setOnClickListener(v->sharePng());
    Intent i=getIntent();if(i!=null){Uri u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u==null)u=i.getData();if(u!=null)load(u);}
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==1&&result==RESULT_OK&&data!=null)load(data.getData());}
  private void load(Uri uri){try{
    String mime=getContentResolver().getType(uri);String name=uri.toString().toLowerCase(Locale.ROOT);
    try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst()){int idx=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(idx>=0)name=cursor.getString(idx).toLowerCase(Locale.ROOT);}}
    if(name.endsWith(".cdr")||(mime!=null&&(mime.toLowerCase(Locale.ROOT).contains("coreldraw")||mime.toLowerCase(Locale.ROOT).contains("zip")))){
      bitmap=readCdrPreview(uri);
      if(bitmap==null){status.setText("CDR reconhecido, mas não foi encontrada prévia PNG compatível. Renderização vetorial ainda indisponível.");preview.setImageDrawable(null);return;}
      preview.setImageBitmap(bitmap);
      status.setText("Prévia incorporada do CDR: "+bitmap.getWidth()+" × "+bitmap.getHeight()+" px. Pode estar desatualizada ou em baixa resolução.");
      return;
    }
    if("application/pdf".equals(mime)||name.endsWith(".pdf")){
      try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");PdfRenderer r=new PdfRenderer(fd)){
        PdfRenderer.Page p=r.openPage(0);float scale=Math.min(2f,2000f/Math.max(p.getWidth(),p.getHeight()));bitmap=Bitmap.createBitmap(Math.max(1,(int)(p.getWidth()*scale)),Math.max(1,(int)(p.getHeight()*scale)),Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);p.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();}
    }else{try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in);}}
    if(bitmap==null)throw new IOException("Formato não suportado nesta versão");preview.setImageBitmap(bitmap);status.setText("Prévia pronta. PNG pode ser compartilhado.");
  }catch(Exception e){bitmap=null;preview.setImageDrawable(null);status.setText("Não foi possível abrir: "+e.getMessage());}}
  private Bitmap readCdrPreview(Uri uri) throws IOException {
    Bitmap best=null; long bestPixels=0;
    try(InputStream raw=getContentResolver().openInputStream(uri);ZipInputStream zip=new ZipInputStream(new BufferedInputStream(raw))){
      ZipEntry entry;
      while((entry=zip.getNextEntry())!=null){
        String n=entry.getName().toLowerCase(Locale.ROOT);
        if(!entry.isDirectory()&&n.startsWith("previews/")&&n.endsWith(".png")&&entry.getSize()<8_000_000){
          ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buf=new byte[8192];int count;
          while((count=zip.read(buf))!=-1){if(bytes.size()+count>8_000_000)break;bytes.write(buf,0,count);}
          byte[] data=bytes.toByteArray();Bitmap candidate=BitmapFactory.decodeByteArray(data,0,data.length);
          if(candidate!=null){long pixels=(long)candidate.getWidth()*candidate.getHeight();if(pixels>bestPixels){if(best!=null)best.recycle();best=candidate;bestPixels=pixels;}else candidate.recycle();}
        }
        zip.closeEntry();
      }
    }
    return best;
  }
  private void sharePng(){if(bitmap==null){Toast.makeText(this,"Abra primeiro uma imagem ou PDF compatível",Toast.LENGTH_LONG).show();return;}
    try{File f=new File(getCacheDir(),"arte-compartilhada.png");try(FileOutputStream out=new FileOutputStream(f)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
      Uri u=FileProvider.getUriForFile(this,getPackageName()+".files",f);Intent send=new Intent(Intent.ACTION_SEND);send.setType("image/png");send.putExtra(Intent.EXTRA_STREAM,u);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(send,"Compartilhar PNG"));
    }catch(Exception e){status.setText("Erro ao compartilhar: "+e.getMessage());}}
}
