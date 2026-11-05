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
  private ZoomImageView preview; private TextView status,pageStatus; private Bitmap bitmap;
  private final java.util.ArrayList<Bitmap> pages=new java.util.ArrayList<>();private int pageIndex=0;
  @Override public void onCreate(Bundle b) { super.onCreate(b);
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,30,20,20);
    TextView title=new TextView(this);title.setText("CDR Viewer · Offline");title.setTextSize(23);root.addView(title);
    status=new TextView(this);status.setText("Abra um CDR para visualizar a prévia incorporada e compartilhar em PNG. A prévia pode ter baixa resolução.");root.addView(status);
    Button open=new Button(this);open.setText("Abrir arquivo");root.addView(open);
    Button share=new Button(this);share.setText("Compartilhar PNG no WhatsApp");root.addView(share);
    Button update=new Button(this);update.setText("Verificar atualizações");root.addView(update);
    update.setOnClickListener(v->new UpdateManager(this).check(true));
    LinearLayout navigation=new LinearLayout(this);navigation.setGravity(android.view.Gravity.CENTER_VERTICAL);
    Button previous=new Button(this);previous.setText("◀");navigation.addView(previous);
    pageStatus=new TextView(this);pageStatus.setText("Nenhuma página");pageStatus.setGravity(android.view.Gravity.CENTER);navigation.addView(pageStatus,new LinearLayout.LayoutParams(0,-2,1));
    Button next=new Button(this);next.setText("▶");navigation.addView(next);root.addView(navigation);
    preview=new ZoomImageView(this);root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    previous.setOnClickListener(v->showPage(pageIndex-1));next.setOnClickListener(v->showPage(pageIndex+1));
    open.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);});
    share.setOnClickListener(v->sharePng());
    Intent i=getIntent();if(i!=null){Uri u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u==null)u=i.getData();if(u!=null)load(u);}
    new UpdateManager(this).check(false);
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==1&&result==RESULT_OK&&data!=null)load(data.getData());}
  private void load(Uri uri){try{
    String mime=getContentResolver().getType(uri);String name=uri.toString().toLowerCase(Locale.ROOT);
    try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst()){int idx=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(idx>=0)name=cursor.getString(idx).toLowerCase(Locale.ROOT);}}
    if(name.endsWith(".cdr")||(mime!=null&&(mime.toLowerCase(Locale.ROOT).contains("coreldraw")||mime.toLowerCase(Locale.ROOT).contains("zip")))){
      pages.clear();pages.addAll(readCdrPreviews(uri));
      bitmap=pages.isEmpty()?null:pages.get(0);
      if(bitmap==null){status.setText("CDR reconhecido, mas não foi encontrada prévia PNG compatível. Renderização vetorial ainda indisponível.");preview.setImageDrawable(null);return;}
      showPage(0);
      status.setText("Prévia incorporada do CDR: "+bitmap.getWidth()+" × "+bitmap.getHeight()+" px. Pode estar desatualizada ou em baixa resolução.");
      return;
    }
    if("application/pdf".equals(mime)||name.endsWith(".pdf")){
      try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");PdfRenderer r=new PdfRenderer(fd)){
        PdfRenderer.Page p=r.openPage(0);float scale=Math.min(2f,2000f/Math.max(p.getWidth(),p.getHeight()));bitmap=Bitmap.createBitmap(Math.max(1,(int)(p.getWidth()*scale)),Math.max(1,(int)(p.getHeight()*scale)),Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);p.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();}
    }else{try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in);}}
    if(bitmap==null)throw new IOException("Formato não suportado nesta versão");pages.clear();pages.add(bitmap);showPage(0);status.setText("Prévia pronta. Amplie com dois dedos ou compartilhe o PNG.");
  }catch(Exception e){bitmap=null;pages.clear();pageStatus.setText("Nenhuma página");preview.setImageDrawable(null);status.setText("Não foi possível abrir: "+e.getMessage());}}
  private void showPage(int index){
    if(index<0||index>=pages.size())return;
    pageIndex=index;bitmap=pages.get(index);preview.setImageBitmap(bitmap);
    pageStatus.setText("Página "+(index+1)+" de "+pages.size());
  }
  private java.util.ArrayList<Bitmap> readCdrPreviews(Uri uri) throws IOException {
    java.util.ArrayList<Bitmap> found=new java.util.ArrayList<>();
    try(InputStream raw=getContentResolver().openInputStream(uri);ZipInputStream zip=new ZipInputStream(new BufferedInputStream(raw))){
      ZipEntry entry;
      while((entry=zip.getNextEntry())!=null){
        String n=entry.getName().toLowerCase(Locale.ROOT);
        if(!entry.isDirectory()&&n.startsWith("previews/")&&n.endsWith(".png")){
          ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buf=new byte[8192];int count;boolean oversized=false;
          while((count=zip.read(buf))!=-1){if(bytes.size()+count>16_000_000){oversized=true;break;}bytes.write(buf,0,count);}
          if(!oversized){byte[] data=bytes.toByteArray();Bitmap candidate=BitmapFactory.decodeByteArray(data,0,data.length);if(candidate!=null)found.add(candidate);}
        }
        zip.closeEntry();
      }
    }
    return found;
  }
  private void sharePng(){if(bitmap==null){Toast.makeText(this,"Abra primeiro uma imagem ou PDF compatível",Toast.LENGTH_LONG).show();return;}
    try{File f=new File(getCacheDir(),"arte-compartilhada.png");try(FileOutputStream out=new FileOutputStream(f)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
      Uri u=FileProvider.getUriForFile(this,getPackageName()+".files",f);Intent send=new Intent(Intent.ACTION_SEND);send.setType("image/png");send.putExtra(Intent.EXTRA_STREAM,u);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(send,"Compartilhar PNG"));
    }catch(Exception e){status.setText("Erro ao compartilhar: "+e.getMessage());}}
}
