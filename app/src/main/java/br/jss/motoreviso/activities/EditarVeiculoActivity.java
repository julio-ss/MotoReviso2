package br.jss.motoreviso.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Veiculo;
import br.jss.motoreviso.utils.SystemBarHelper;

public class EditarVeiculoActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_editar_veiculo);

        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        veiculoId = getIntent().getStringExtra("VEICULO_ID");
        if (veiculoId == null) {
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
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
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagemLauncher.launch(intent);
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

        if (veiculoAtual.getUrlImagemPrincipal() != null && !veiculoAtual.getUrlImagemPrincipal().isEmpty()) {
            Glide.with(this)
                    .load(veiculoAtual.getUrlImagemPrincipal())
                    .centerCrop()
                    .placeholder(R.drawable.ic_car_modern)
                    .error(R.drawable.ic_car_modern)
                    .into(imgVeiculo);
        } else {
            imgVeiculo.setImageResource(R.drawable.ic_car_modern);
        }
    }

    private void salvarAlteracoes() {
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

        if (imagemUri != null) {
            progressEditar.setVisibility(android.view.View.VISIBLE);
            String nomeImagem = "veiculo_" + veiculoId + "_" + System.currentTimeMillis();
            firebaseManager.uploadImagemVeiculo(imagemUri, nomeImagem, new FirebaseManager.OnUploadCompleteListener() {
                @Override
                public void onUploadComplete(String downloadUrl) {
                    veiculoAtual.setUrlImagemPrincipal(downloadUrl);
                    atualizarVeiculoFirebase();
                }

                @Override
                public void onUploadFailed(Exception exception) {
                    progressEditar.setVisibility(android.view.View.GONE);
                    Toast.makeText(EditarVeiculoActivity.this, "Erro ao fazer upload: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            atualizarVeiculoFirebase();
        }
    }

    private void atualizarVeiculoFirebase() {
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