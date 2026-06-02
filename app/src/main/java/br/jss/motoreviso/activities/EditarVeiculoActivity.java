package br.jss.motoreviso.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Veiculo;
import br.jss.motoreviso.utils.ImagemLoader;
import br.jss.motoreviso.utils.SystemBarHelper;

public class EditarVeiculoActivity extends AppCompatActivity {
    private static final String TAG = "EditarVeiculoActivity";

    private String veiculoId;
    private EditText edtMarca, edtModelo, edtPlaca, edtKmAtual, edtDescricao;
    private Button btnSalvar, btnCancelar;
    private ImageView imgVeiculo;
    private FloatingActionButton fabAlterarFoto;
    private ProgressBar progressEditar;
    private FirebaseManager firebaseManager;
    private Veiculo veiculoAtual;
    private Uri imagemUri;
    private ActivityResultLauncher<Intent> imagemLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");
        setContentView(R.layout.activity_editar_veiculo);

        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        veiculoId = getIntent().getStringExtra("VEICULO_ID");
        Log.d(TAG, "Veiculo ID: " + veiculoId);

        if (veiculoId == null) {
            Log.w(TAG, "Veiculo ID is null, finishing activity");
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "Current user on onCreate: " + (currentUser != null ? currentUser.getEmail() : "NULL"));

        inicializarViews();
        setupImagemLauncher();
        carregarVeiculo();

        btnSalvar.setOnClickListener(v -> salvarAlteracoes());
        btnCancelar.setOnClickListener(v -> finish());
        fabAlterarFoto.setOnClickListener(v -> selecionarImagem());
    }

    private void inicializarViews() {
        edtMarca = findViewById(R.id.edt_marca_editar);
        edtModelo = findViewById(R.id.edt_modelo_editar);
        edtPlaca = findViewById(R.id.edt_placa_editar);
        edtKmAtual = findViewById(R.id.edt_km_atual_editar);
        edtDescricao = findViewById(R.id.edt_descricao_editar);
        btnSalvar = findViewById(R.id.btn_salvar_editar);
        btnCancelar = findViewById(R.id.btn_cancelar_editar);
        imgVeiculo = findViewById(R.id.img_veiculo_editar);
        fabAlterarFoto = findViewById(R.id.fab_alterar_foto);
        progressEditar = findViewById(R.id.progress_editar);
    }

    private void setupImagemLauncher() {
        imagemLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imagemUri = result.getData().getData();
                        if (imagemUri != null) {
                            imgVeiculo.setImageURI(imagemUri);
                            Toast.makeText(this, "Imagem selecionada", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void selecionarImagem() {
        // Check if permission is granted
        String permissao = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? Manifest.permission.READ_MEDIA_IMAGES
            : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permissao) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{permissao}, 101);
        } else {
            // Permission already granted, open gallery
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagemLauncher.launch(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open gallery
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagemLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Permissão de acesso à galeria foi negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void carregarVeiculo() {
        firebaseManager.obterVeiculo(veiculoId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                veiculoAtual = task.getResult().toObject(Veiculo.class);
                if (veiculoAtual != null) {
                    veiculoAtual.setId(veiculoId);
                    preencherCampos();
                }
            } else {
                Toast.makeText(this, "Erro ao carregar veículo", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void preencherCampos() {
        edtMarca.setText(veiculoAtual.getMarca() != null ? veiculoAtual.getMarca() : "");
        edtModelo.setText(veiculoAtual.getModelo() != null ? veiculoAtual.getModelo() : "");
        edtPlaca.setText(veiculoAtual.getPlaca() != null ? veiculoAtual.getPlaca() : "");
        edtKmAtual.setText(veiculoAtual.getKmAtual() != null ? veiculoAtual.getKmAtual().toString() : "0");
        edtDescricao.setText(veiculoAtual.getDescricao() != null ? veiculoAtual.getDescricao() : "");

        ImagemLoader.carregarImagem(this, imgVeiculo, veiculoAtual.getUrlImagemPrincipal());
    }

    private void salvarAlteracoes() {
        Log.d(TAG, "salvarAlteracoes() called");

        String marca = edtMarca.getText().toString().trim();
        String modelo = edtModelo.getText().toString().trim();
        String placa = edtPlaca.getText().toString().trim();

        if (marca.isEmpty() || modelo.isEmpty() || placa.isEmpty()) {
            Toast.makeText(this, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        veiculoAtual.setMarca(marca);
        veiculoAtual.setModelo(modelo);
        veiculoAtual.setPlaca(placa);

        try {
            veiculoAtual.setKmAtual(Long.parseLong(edtKmAtual.getText().toString()));
        } catch (NumberFormatException e) {
            veiculoAtual.setKmAtual(0L);
        }

        veiculoAtual.setDescricao(edtDescricao.getText().toString());

        // Se uma nova imagem foi selecionada, fazer upload para Firebase Storage
        if (imagemUri != null) {
            Log.d(TAG, "imagemUri is not null, checking authentication...");

            // Verificar se usuário está autenticado
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            Log.d(TAG, "FirebaseAuth instance obtained");

            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            Log.d(TAG, "Current user: " + (currentUser != null ? currentUser.getUid() : "NULL"));

            if (currentUser == null) {
                Log.w(TAG, "User is not authenticated!");
                Toast.makeText(this, "Você precisa estar autenticado para fazer upload de imagem", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "User authenticated as: " + currentUser.getEmail());
            progressEditar.setVisibility(android.view.View.VISIBLE);

            // Gerar nome único para a imagem
            String nomeImagem = "veiculo_" + veiculoId + "_" + System.currentTimeMillis() + ".jpg";
            Log.d(TAG, "Starting image upload: " + nomeImagem);

            // Upload para Firebase Storage e obter URL
            firebaseManager.uploadImagemVeiculo(imagemUri, nomeImagem,
                new FirebaseManager.OnUploadCompleteListener() {
                    @Override
                    public void onUploadComplete(String downloadUrl) {
                        Log.d(TAG, "Image upload completed, URL: " + downloadUrl);
                        // Salvar URL de download no veículo
                        veiculoAtual.setUrlImagemPrincipal(downloadUrl);
                        atualizarVeiculoFirebase();
                    }

                    @Override
                    public void onUploadFailed(Exception exception) {
                        Log.e(TAG, "Image upload failed", exception);
                        progressEditar.setVisibility(android.view.View.GONE);
                        String mensagemErro = "Erro ao fazer upload da imagem";

                        // Mensagens de erro mais descritivas
                        if (exception.getMessage() != null) {
                            if (exception.getMessage().contains("Permission denied")) {
                                mensagemErro = "Permissão negada para acessar a imagem";
                            } else if (exception.getMessage().contains("404")) {
                                mensagemErro = "Firebase Storage não configurado corretamente";
                            } else if (exception.getMessage().contains("SecurityException")) {
                                mensagemErro = "Permissão de acesso à mídia foi negada";
                            } else {
                                mensagemErro += ": " + exception.getMessage();
                            }
                        }

                        Toast.makeText(EditarVeiculoActivity.this, mensagemErro, Toast.LENGTH_LONG).show();
                    }
                });
        } else {
            Log.d(TAG, "No image selected, updating vehicle without image upload");
            // Se nenhuma imagem foi selecionada, apenas atualizar os dados
            atualizarVeiculoFirebase();
        }
    }

    private void atualizarVeiculoFirebase() {
        progressEditar.setVisibility(android.view.View.VISIBLE);
        firebaseManager.atualizarVeiculo(veiculoId, veiculoAtual)
                .addOnSuccessListener(aVoid -> {
                    progressEditar.setVisibility(android.view.View.GONE);
                    Toast.makeText(EditarVeiculoActivity.this, "Veículo atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressEditar.setVisibility(android.view.View.GONE);
                    Toast.makeText(EditarVeiculoActivity.this, "Erro ao atualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}