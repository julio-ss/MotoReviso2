package br.jss.motoreviso.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Equipamento;
import br.jss.motoreviso.utils.KeyboardScrollHelper;
import br.jss.motoreviso.utils.SystemBarHelper;

public class CadastroEquipamentoActivity extends AppCompatActivity {

    private String pilotoId;
    private String equipamentoId;
    private Uri fotoUri;

    // Dois Calendars separados para evitar conflito de estado
    private Calendar calendarCompra  = Calendar.getInstance();
    private Calendar calendarValidade = Calendar.getInstance();

    // Views
    private AutoCompleteTextView acvTipo;
    private TextInputEditText edtMarca, edtModelo, edtTamanho, edtCor;
    private TextInputEditText edtDataCompra, edtDataValidade, edtNotas;
    private ImageView imgFotoEquipamento;
    private MaterialButton btnSalvar, btnCancelar, btnSelecionarFoto;

    private FirebaseManager firebaseManager;
    private ActivityResultLauncher<Intent> galeriaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_equipamento);
        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        pilotoId     = getIntent().getStringExtra("PILOTO_ID");
        equipamentoId = getIntent().getStringExtra("EQUIPAMENTO_ID");

        if (pilotoId == null) {
            Toast.makeText(this, "Erro: piloto não identificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();

        inicializarViews();
        setupDropdownTipo();
        setupGaleriaLauncher();

        ScrollView scrollView = findViewById(R.id.scroll_view_equipamento);
        KeyboardScrollHelper.setupKeyboardScrollForScrollView(scrollView);

        if (equipamentoId != null) {
            carregarEquipamentoExistente();
        }

        btnSalvar.setOnClickListener(v -> salvarEquipamento());
        btnCancelar.setOnClickListener(v -> finish());
        btnSelecionarFoto.setOnClickListener(v -> abrirGaleria());
    }

    private void inicializarViews() {
        acvTipo             = findViewById(R.id.acv_tipo_equipamento);
        edtMarca            = findViewById(R.id.edt_marca_equipamento);
        edtModelo           = findViewById(R.id.edt_modelo_equipamento);
        edtTamanho          = findViewById(R.id.edt_tamanho_equipamento);
        edtCor              = findViewById(R.id.edt_cor_equipamento);
        edtDataCompra       = findViewById(R.id.edt_data_compra);
        edtDataValidade     = findViewById(R.id.edt_data_validade);
        edtNotas            = findViewById(R.id.edt_notas_equipamento);
        imgFotoEquipamento  = findViewById(R.id.img_foto_equipamento);
        btnSalvar           = findViewById(R.id.btn_salvar_equipamento);
        btnCancelar         = findViewById(R.id.btn_cancelar_equipamento);
        btnSelecionarFoto   = findViewById(R.id.btn_selecionar_foto_equipamento);

        // Date pickers com Calendars separados
        edtDataCompra.setOnClickListener(v -> mostrarDatePicker(true));
        edtDataValidade.setOnClickListener(v -> mostrarDatePicker(false));
    }

    private void setupDropdownTipo() {
        String[] tipos = getResources().getStringArray(R.array.tipos_equipamento);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, tipos);
        acvTipo.setAdapter(adapter);
    }

    private void setupGaleriaLauncher() {
        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        fotoUri = result.getData().getData();
                        Glide.with(this).load(fotoUri).centerCrop().into(imgFotoEquipamento);
                    }
                });
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galeriaLauncher.launch(intent);
    }

    private void mostrarDatePicker(boolean paraCompra) {
        Calendar cal = paraCompra ? calendarCompra : calendarValidade;
        android.widget.DatePicker datePicker = new android.widget.DatePicker(this);
        datePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH), null);

        new AlertDialog.Builder(this)
                .setTitle(paraCompra ? "Data de Compra" : "Data de Validade")
                .setView(datePicker)
                .setPositiveButton("OK", (d, w) -> {
                    cal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                    String dataStr = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"))
                            .format(cal.getTime());
                    if (paraCompra) {
                        edtDataCompra.setText(dataStr);
                    } else {
                        edtDataValidade.setText(dataStr);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void salvarEquipamento() {
        String tipo = acvTipo.getText().toString().trim();
        if (tipo.isEmpty()) {
            acvTipo.setError("Tipo obrigatório");
            acvTipo.requestFocus();
            return;
        }

        Equipamento equip = new Equipamento();
        equip.setPilotoId(pilotoId);
        equip.setTipo(tipo);
        equip.setMarca(getText(edtMarca));
        equip.setModelo(getText(edtModelo));
        equip.setTamanho(getText(edtTamanho));
        equip.setCor(getText(edtCor));
        equip.setNotas(getText(edtNotas));

        if (!getText(edtDataCompra).isEmpty()) {
            equip.setDataCompra(calendarCompra.getTimeInMillis());
        }
        if (!getText(edtDataValidade).isEmpty()) {
            equip.setDataValidade(calendarValidade.getTimeInMillis());
        }

        if (fotoUri != null) {
            equip.setFotoUrl(fotoUri.toString());
        }

        equip.setDataCadastro(System.currentTimeMillis());

        firebaseManager.adicionarEquipamento(equip)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, R.string.equipamento_salvo, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void carregarEquipamentoExistente() {
        // Para uso futuro (edição de equipamentos)
    }

    private String getText(TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }
}
