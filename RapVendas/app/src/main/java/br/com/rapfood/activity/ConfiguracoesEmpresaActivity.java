package br.com.rapfood.activity;

import static br.com.rapfood.helper.ConfiguracaoFirebase.getFirebaseStorage;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.net.Uri;

import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;

import android.view.View.OnClickListener;
import android.widget.EditText;

import android.widget.Toast;


import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.io.ByteArrayOutputStream;
import java.util.Locale;

import br.com.rapfood.R;
import br.com.rapfood.helper.ConfiguracaoFirebase;
import br.com.rapfood.helper.Permissoes;
import br.com.rapfood.helper.UsuarioFirebase;
import br.com.rapfood.model.Empresa;
import de.hdodenhof.circleimageview.CircleImageView;

public class ConfiguracoesEmpresaActivity extends AppCompatActivity {



    private EditText editEmpresaNome, editEmpresaCategoria,
            editEmpresaTempo, editEmpresaTaxa;
    private CircleImageView imagePerfilEmpresa;
    private  String[] permissoes = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE

    };


    private static final int SELECAO_GALERIA = 200;
    private StorageReference storageReference;
    private DatabaseReference firebaseRef;

    private String idEmpresaLogado;
    private String urlImagemSelecionada = "";

    @SuppressLint("QueryPermissionsNeeded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {




        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes_empresa);

        //Configurações iniciais
        Permissoes.validarPermissoes(permissoes,this, 1);
        inicializarComponentes();
        storageReference = getFirebaseStorage();

        firebaseRef = ConfiguracaoFirebase.getFirebase();
        idEmpresaLogado = UsuarioFirebase.getIdUsuario();

        //Configurações Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Configurações");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        //acessando local de imagem do usuario

        imagePerfilEmpresa.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    startActivityForResult(i, SELECAO_GALERIA);



            }
        });


        /*Recuperar dados da empresa*/
        recuperarDadosEmpresa();


    }

    private void recuperarDadosEmpresa(){

        DatabaseReference empresaRef = firebaseRef
                .child("empresas")
                .child( idEmpresaLogado );

        empresaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if( dataSnapshot.getValue() != null ){
                    Empresa empresas = dataSnapshot.getValue(Empresa.class);
                    editEmpresaNome.setText(empresas.getNome());
                    editEmpresaCategoria.setText(empresas.getCategoria());
                    editEmpresaTaxa.setText(empresas.getPrecoEntrega().toString());
                    editEmpresaTempo.setText(empresas.getTempo());

                    urlImagemSelecionada = empresas.getUrlImagem();
                    if( urlImagemSelecionada!=("")){

                        Picasso.get().load(urlImagemSelecionada)
                                .into(imagePerfilEmpresa);
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void validarDadosEmpresa(View view){


        //Valida se os campos foram preenchidos
        String nome = editEmpresaNome.getText().toString();
        String taxa = editEmpresaTaxa.getText().toString();
        String categoria = editEmpresaCategoria.getText().toString();
        String tempo = editEmpresaTempo.getText().toString();

        if( !nome.isEmpty()){
            if( !taxa.isEmpty()){
                if( !categoria.isEmpty()){
                    if( !tempo.isEmpty()){
// recupera da Model empresa, os atributos da empresa
                        Empresa empresa = new Empresa();
                        empresa.setIdUsuario( idEmpresaLogado );
                        empresa.setNome( nome );
                        empresa.setPrecoEntrega( Double.parseDouble(taxa) );
                        empresa.setCategoria(categoria);
                        empresa.setTempo( tempo );
                        empresa.setUrlImagem( urlImagemSelecionada );
                        empresa.salvar();
                        finish();

                    }else{
                        exibirMensagem("Digite um tempo de entrega");
                    }
                }else{
                    exibirMensagem("Digite uma categoria");
                }
            }else{
                exibirMensagem("Digite uma taxa de entrega");
            }
        }else{
            exibirMensagem("Digite um nome para a empresa");
        }

    }

    private void exibirMensagem(String texto){
        Toast.makeText(this, texto, Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( resultCode == RESULT_OK){
            Bitmap imagem = null;

            try {

                switch (requestCode) {
                    case SELECAO_GALERIA:
                        Uri localImagem = data.getData();
                        imagem = MediaStore.Images
                                .Media
                                .getBitmap(
                                        getContentResolver(),
                                        localImagem
                                );
                        break;
                }

                if( imagem != null){

                    imagePerfilEmpresa.setImageBitmap( imagem );

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] dadosImagem = baos.toByteArray();

                    final StorageReference imagemRef = storageReference.child("imagens")
                            .child("empresas")
                            .child(idEmpresaLogado + "jpeg");

                    UploadTask uploadTask = imagemRef.putBytes( dadosImagem );
                    uploadTask.addOnFailureListener(e -> Toast.makeText(ConfiguracoesEmpresaActivity.this,
                            "Erro ao fazer upload da imagem",

                            Toast.LENGTH_SHORT).show())
                            .addOnSuccessListener(ConfiguracoesEmpresaActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {

                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            imagemRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Task<Uri> task = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                                    urlImagemSelecionada = uri.toString();
                                    Toast.makeText(ConfiguracoesEmpresaActivity.this,
                                            "Sucesso ao fazer upload da imagem",
                                            Toast.LENGTH_SHORT).show();

                                }
                            });

                        }
                    });

                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }

    }

    private void inicializarComponentes(){
        editEmpresaNome = findViewById(R.id.editEmpresaNome);
        editEmpresaCategoria = findViewById(R.id.editEmpresaCategoria);
        editEmpresaTaxa = findViewById(R.id.editEmpresaTaxa);
        editEmpresaTempo = findViewById(R.id.editEmpresaTempo);
        imagePerfilEmpresa = findViewById(R.id.imagePerfilEmpres);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int permissaoResultado : grantResults) {
            if (permissaoResultado == PackageManager.PERMISSION_DENIED) {
                alertaValidacaoPermissao();
            }
        }
    }
    private void alertaValidacaoPermissao(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissões Negadas");
        builder.setMessage("Para utilizar o app é necessário aceitar as permissões");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }
}