package br.jss.motoreviso.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
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
import br.jss.motoreviso.models.Piloto;
import br.jss.motoreviso.utils.KeyboardScrollHelper;
import br.jss.motoreviso.utils.SystemBarHelper;

public class CadastroPilotoActivity extends AppCompatActivity {

    private static final String[] CORES_PRESET = {
            "#FF4D4D", "#FF8C00", "#FFD700", "#1FD68B",
            "#00A8FF", "#7C4DFF", "#E040FB", "#FF4081",
            "#FFFFFF", "#B0BEC5", "#37474F", "#000000"
    };

    private String pilotoId;
    private Calendar calendarNascimento = Calendar.getInstance();
    private String avatarCor1 = "#00A8FF";
    private String avatarCor2 = "#FFFFFF";

    // Views
    private TextInputEditText edtNome, edtApelido, edtNumero, edtDataNascimento;
    private TextInputEditText edtPeso, edtAltura, edtNacionalidade, edtCidade;
    private TextInputEditText edtTelefone, edtEmail, edtContatoEmergencia;
    private TextInputEditText edtAnosExperiencia, edtEquipe;
    private TextInputEditText edtNumCorridas, edtNumVitorias, edtNumQuedas;
    private TextInputEditText edtMelhorTempo, edtObservacoes;
    private AutoCompleteTextView acvCategoria, acvTipoSanguineo;
    private FrameLayout frameAvatarPreview;
    private TextView tvAvatarNumeroPreview;
    private MaterialButton btnCorFundo, btnCorNumero, btnSalvar, btnCancelar;

    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_piloto);
        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        pilotoId = getIntent().getStringExtra("PILOTO_ID");
        firebaseManager = FirebaseManager.getInstance();

        inicializarViews();
        setupDropdowns();
        setupAvatarPreview();

        ScrollView scrollView = findViewById(R.id.scroll_view_piloto);
        KeyboardScrollHelper.setupKeyboardScrollForScrollView(scrollView);

        // Modo edição: carregar dados existentes
        if (pilotoId != null) {
            TextView tvTitulo = findViewById(R.id.tv_titulo_cadastro_piloto);
            tvTitulo.setText(R.string.editar_piloto);
            carregarPilotoExistente();
        }

        btnSalvar.setOnClickListener(v -> salvarPiloto());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void inicializarViews() {
        edtNome               = findViewById(R.id.edt_nome_piloto);
        edtApelido            = findViewById(R.id.edt_apelido_piloto);
        edtNumero             = findViewById(R.id.edt_numero_piloto);
        edtDataNascimento     = findViewById(R.id.edt_data_nascimento);
        edtPeso               = findViewById(R.id.edt_peso);
        edtAltura             = findViewById(R.id.edt_altura);
        edtNacionalidade      = findViewById(R.id.edt_nacionalidade);
        edtCidade             = findViewById(R.id.edt_cidade_piloto);
        edtTelefone           = findViewById(R.id.edt_telefone_piloto);
        edtEmail              = findViewById(R.id.edt_email_piloto);
        edtContatoEmergencia  = findViewById(R.id.edt_contato_emergencia);
        edtAnosExperiencia    = findViewById(R.id.edt_anos_experiencia);
        edtEquipe             = findViewById(R.id.edt_equipe);
        edtNumCorridas        = findViewById(R.id.edt_num_corridas);
        edtNumVitorias        = findViewById(R.id.edt_num_vitorias);
        edtNumQuedas          = findViewById(R.id.edt_num_quedas);
        edtMelhorTempo        = findViewById(R.id.edt_melhor_tempo);
        edtObservacoes        = findViewById(R.id.edt_observacoes_piloto);
        acvCategoria          = findViewById(R.id.acv_categoria_piloto);
        acvTipoSanguineo      = findViewById(R.id.acv_tipo_sanguineo);
        frameAvatarPreview    = findViewById(R.id.frame_avatar_preview);
        tvAvatarNumeroPreview = findViewById(R.id.tv_avatar_numero_preview);
        btnCorFundo           = findViewById(R.id.btn_cor_fundo);
        btnCorNumero          = findViewById(R.id.btn_cor_numero);
        btnSalvar             = findViewById(R.id.btn_salvar_piloto);
        btnCancelar           = findViewById(R.id.btn_cancelar_piloto);

        // Date picker
        edtDataNascimento.setOnClickListener(v -> mostrarDatePicker());

        // Atualizar preview do número em tempo real
        edtNumero.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvAvatarNumeroPreview.setText(s.length() > 0 ? s.toString() : "#");
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setupDropdowns() {
        // Categorias
        String[] categorias = getResources().getStringArray(R.array.categorias_piloto);
        ArrayAdapter<String> adapterCat = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categorias);
        acvCategoria.setAdapter(adapterCat);

        // Tipos sanguíneos
        String[] tiposSanguineos = getResources().getStringArray(R.array.tipos_sanguineos);
        ArrayAdapter<String> adapterSang = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, tiposSanguineos);
        acvTipoSanguineo.setAdapter(adapterSang);
    }

    private void setupAvatarPreview() {
        atualizarAvatarPreview();

        btnCorFundo.setOnClickListener(v -> mostrarColorPicker(true));
        btnCorNumero.setOnClickListener(v -> mostrarColorPicker(false));
    }

    private void atualizarAvatarPreview() {
        try {
            GradientDrawable bg = (GradientDrawable) frameAvatarPreview.getBackground().mutate();
            bg.setColor(Color.parseColor(avatarCor1));
        } catch (Exception ignored) {
            try {
                GradientDrawable bg = (GradientDrawable) frameAvatarPreview.getBackground().mutate();
                bg.setColor(0xFF00A8FF);
            } catch (Exception ignored2) {}
        }

        try {
            tvAvatarNumeroPreview.setTextColor(Color.parseColor(avatarCor2));
        } catch (Exception ignored) {
            tvAvatarNumeroPreview.setTextColor(Color.WHITE);
        }
    }

    private void mostrarColorPicker(boolean paraFundo) {
        // GridView com 12 cores pré-definidas
        GridView gridView = new GridView(this);
        gridView.setNumColumns(4);
        gridView.setPadding(16, 16, 16, 16);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, CORES_PRESET) {
            @Override
            public android.view.View getView(int position, android.view.View convertView,
                                              android.view.ViewGroup parent) {
                android.view.View colorView = new android.view.View(getContext());
                int size = (int) (56 * getResources().getDisplayMetrics().density);
                colorView.setLayoutParams(new android.widget.AbsListView.LayoutParams(size, size));
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.OVAL);
                try {
                    drawable.setColor(Color.parseColor(CORES_PRESET[position]));
                } catch (Exception ignored) {
                    drawable.setColor(0xFF00A8FF);
                }
                colorView.setBackground(drawable);
                return colorView;
            }
        };
        gridView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(paraFundo ? "Cor do Fundo do Avatar" : "Cor do Número")
                .setView(gridView)
                .setNegativeButton("Cancelar", null)
                .create();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (paraFundo) {
                avatarCor1 = CORES_PRESET[position];
            } else {
                avatarCor2 = CORES_PRESET[position];
            }
            atualizarAvatarPreview();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void mostrarDatePicker() {
        android.widget.DatePicker datePicker = new android.widget.DatePicker(this);
        datePicker.init(calendarNascimento.get(Calendar.YEAR),
                        calendarNascimento.get(Calendar.MONTH),
                        calendarNascimento.get(Calendar.DAY_OF_MONTH),
                        null);

        new AlertDialog.Builder(this)
                .setTitle("Data de Nascimento")
                .setView(datePicker)
                .setPositiveButton("OK", (d, w) -> {
                    calendarNascimento.set(datePicker.getYear(),
                                          datePicker.getMonth(),
                                          datePicker.getDayOfMonth());
                    String dataFormatada = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"))
                            .format(calendarNascimento.getTime());
                    edtDataNascimento.setText(dataFormatada);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void salvarPiloto() {
        String nome = getText(edtNome);
        if (nome.isEmpty()) {
            edtNome.setError("Nome obrigatório");
            edtNome.requestFocus();
            return;
        }

        Piloto piloto = new Piloto();
        piloto.setNomeCompleto(nome);
        piloto.setApelido(getText(edtApelido));
        piloto.setNumeroPiloto(getText(edtNumero));

        // Data nascimento
        String dataNascStr = getText(edtDataNascimento);
        if (!dataNascStr.isEmpty()) {
            piloto.setDataNascimento(calendarNascimento.getTimeInMillis());
        }

        // Peso e altura como Double
        String pesoStr = getText(edtPeso);
        if (!pesoStr.isEmpty()) {
            try { piloto.setPeso(Double.parseDouble(pesoStr.replace(",", "."))); }
            catch (NumberFormatException ignored) {}
        }
        String alturaStr = getText(edtAltura);
        if (!alturaStr.isEmpty()) {
            try { piloto.setAltura(Double.parseDouble(alturaStr.replace(",", "."))); }
            catch (NumberFormatException ignored) {}
        }

        piloto.setTipoSanguineo(acvTipoSanguineo.getText().toString().trim());
        piloto.setNacionalidade(getText(edtNacionalidade));
        piloto.setCidade(getText(edtCidade));
        piloto.setTelefone(getText(edtTelefone));
        piloto.setEmail(getText(edtEmail));
        piloto.setContatoEmergencia(getText(edtContatoEmergencia));
        piloto.setCategoria(acvCategoria.getText().toString().trim());

        String anosStr = getText(edtAnosExperiencia);
        if (!anosStr.isEmpty()) {
            try { piloto.setAnosExperiencia(Long.parseLong(anosStr)); }
            catch (NumberFormatException ignored) {}
        }

        piloto.setEquipe(getText(edtEquipe));

        String corridasStr = getText(edtNumCorridas);
        if (!corridasStr.isEmpty()) {
            try { piloto.setNumCorridas(Long.parseLong(corridasStr)); }
            catch (NumberFormatException ignored) {}
        }
        String vitoriasStr = getText(edtNumVitorias);
        if (!vitoriasStr.isEmpty()) {
            try { piloto.setNumVitorias(Long.parseLong(vitoriasStr)); }
            catch (NumberFormatException ignored) {}
        }
        String quedasStr = getText(edtNumQuedas);
        if (!quedasStr.isEmpty()) {
            try { piloto.setNumQuedas(Long.parseLong(quedasStr)); }
            catch (NumberFormatException ignored) {}
        }

        piloto.setMelhorTempo(getText(edtMelhorTempo));
        piloto.setObservacoes(getText(edtObservacoes));
        piloto.setAvatarCor1(avatarCor1);
        piloto.setAvatarCor2(avatarCor2);

        piloto.setDataCadastro(System.currentTimeMillis());

        if (pilotoId != null) {
            // Modo edição
            piloto.setId(pilotoId);
            firebaseManager.atualizarPiloto(piloto)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, R.string.piloto_salvo, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Novo piloto
            firebaseManager.adicionarPiloto(piloto)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, R.string.piloto_salvo, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void carregarPilotoExistente() {
        firebaseManager.obterPiloto(pilotoId).addOnSuccessListener(doc -> {
            if (doc == null || !doc.exists()) return;
            Piloto piloto = doc.toObject(Piloto.class);
            if (piloto == null) return;

            setText(edtNome, piloto.getNomeCompleto());
            setText(edtApelido, piloto.getApelido());
            setText(edtNumero, piloto.getNumeroPiloto());
            setText(edtNacionalidade, piloto.getNacionalidade());
            setText(edtCidade, piloto.getCidade());
            setText(edtTelefone, piloto.getTelefone());
            setText(edtEmail, piloto.getEmail());
            setText(edtContatoEmergencia, piloto.getContatoEmergencia());
            setText(edtEquipe, piloto.getEquipe());
            setText(edtMelhorTempo, piloto.getMelhorTempo());
            setText(edtObservacoes, piloto.getObservacoes());

            if (piloto.getPeso() != null) edtPeso.setText(String.valueOf(piloto.getPeso()));
            if (piloto.getAltura() != null) edtAltura.setText(String.valueOf(piloto.getAltura()));
            if (piloto.getAnosExperiencia() != null)
                edtAnosExperiencia.setText(String.valueOf(piloto.getAnosExperiencia()));
            if (piloto.getNumCorridas() != null)
                edtNumCorridas.setText(String.valueOf(piloto.getNumCorridas()));
            if (piloto.getNumVitorias() != null)
                edtNumVitorias.setText(String.valueOf(piloto.getNumVitorias()));
            if (piloto.getNumQuedas() != null)
                edtNumQuedas.setText(String.valueOf(piloto.getNumQuedas()));

            if (piloto.getDataNascimento() != null) {
                calendarNascimento.setTimeInMillis(piloto.getDataNascimento());
                edtDataNascimento.setText(new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"))
                        .format(new Date(piloto.getDataNascimento())));
            }

            if (piloto.getCategoria() != null) acvCategoria.setText(piloto.getCategoria(), false);
            if (piloto.getTipoSanguineo() != null) acvTipoSanguineo.setText(piloto.getTipoSanguineo(), false);

            if (piloto.getAvatarCor1() != null) avatarCor1 = piloto.getAvatarCor1();
            if (piloto.getAvatarCor2() != null) avatarCor2 = piloto.getAvatarCor2();
            atualizarAvatarPreview();

        });
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private String getText(TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }

    private void setText(TextInputEditText edt, String value) {
        if (value != null) edt.setText(value);
    }
}
