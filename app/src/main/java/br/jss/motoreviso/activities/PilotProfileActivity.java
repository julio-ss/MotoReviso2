package br.jss.motoreviso.activities;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import br.jss.motoreviso.R;
import br.jss.motoreviso.adapters.EquipamentoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Equipamento;
import br.jss.motoreviso.models.Piloto;
import br.jss.motoreviso.utils.SystemBarHelper;
import br.jss.motoreviso.views.AvatarView;

public class PilotProfileActivity extends AppCompatActivity {

    private String pilotoId;
    private Piloto pilotoAtual;
    private List<Equipamento> equipamentos;
    private EquipamentoAdapter equipamentoAdapter;

    // Avatar: preview bitmap salvo OU AvatarView com overlays (fallback)
    private ImageView    imgAvatarPreview;
    private FrameLayout  frameAvatarFallback;
    private AvatarView   avatarViewPerfil;
    private ImageView    imgPerfilCapaceteOverlay, imgPerfilJaquetaOverlay;
    private ImageView    imgPerfilCalcaOverlay, imgPerfilBotasOverlay;

    // Texto
    private TextView tvNomePerfil, tvApelidoPerfil, tvCategoriaPerfil, tvIdadePerfil;
    private TextView tvCorridasStat, tvVitoriasStat, tvQuedasStat, tvMelhorTempoStat;

    // Equipamentos
    private RecyclerView recyclerEquipamentos;
    private LinearLayout layoutSemEquipamentos;
    private TextView     tvQtdEquipamentos;

    private MaterialButton         btnEditarPiloto;
    private FloatingActionButton   fabAddEquipamento;
    private FirebaseManager        firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pilot_profile);
        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        pilotoId = getIntent().getStringExtra("PILOTO_ID");
        if (pilotoId == null) { finish(); return; }

        firebaseManager = FirebaseManager.getInstance();
        equipamentos    = new ArrayList<>();

        inicializarViews();
        setupRecyclerEquipamentos();

        MaterialButton btnAvatarPiloto = findViewById(R.id.btn_avatar_piloto);

        btnEditarPiloto.setOnClickListener(v -> {
            Intent intent = new Intent(this, CadastroPilotoActivity.class);
            intent.putExtra("PILOTO_ID", pilotoId);
            startActivity(intent);
        });

        btnAvatarPiloto.setOnClickListener(v -> {
            Intent intent = new Intent(this, AvatarPilotoActivity.class);
            intent.putExtra("PILOTO_ID", pilotoId);
            startActivity(intent);
        });

        fabAddEquipamento.setOnClickListener(v -> {
            Intent intent = new Intent(this, CadastroEquipamentoActivity.class);
            intent.putExtra("PILOTO_ID", pilotoId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarPiloto();
        carregarEquipamentos();
    }

    private void inicializarViews() {
        imgAvatarPreview           = findViewById(R.id.img_avatar_preview);
        frameAvatarFallback        = findViewById(R.id.frame_avatar_perfil_fallback);
        avatarViewPerfil           = findViewById(R.id.avatar_view_perfil);
        imgPerfilCapaceteOverlay   = findViewById(R.id.img_perfil_capacete_overlay);
        imgPerfilJaquetaOverlay    = findViewById(R.id.img_perfil_jaqueta_overlay);
        imgPerfilCalcaOverlay      = findViewById(R.id.img_perfil_calca_overlay);
        imgPerfilBotasOverlay      = findViewById(R.id.img_perfil_botas_overlay);
        tvNomePerfil          = findViewById(R.id.tv_nome_perfil);
        tvApelidoPerfil       = findViewById(R.id.tv_apelido_perfil);
        tvCategoriaPerfil     = findViewById(R.id.tv_categoria_perfil);
        tvIdadePerfil         = findViewById(R.id.tv_idade_perfil);
        tvCorridasStat        = findViewById(R.id.tv_corridas_stat);
        tvVitoriasStat        = findViewById(R.id.tv_vitorias_stat);
        tvQuedasStat          = findViewById(R.id.tv_quedas_stat);
        tvMelhorTempoStat     = findViewById(R.id.tv_melhor_tempo_stat);
        recyclerEquipamentos  = findViewById(R.id.recycler_equipamentos_perfil);
        layoutSemEquipamentos = findViewById(R.id.layout_sem_equipamentos);
        tvQtdEquipamentos     = findViewById(R.id.tv_qtd_equipamentos);
        btnEditarPiloto       = findViewById(R.id.btn_editar_piloto);
        fabAddEquipamento     = findViewById(R.id.fab_add_equipamento);
    }

    private void setupRecyclerEquipamentos() {
        equipamentoAdapter = new EquipamentoAdapter(equipamentos, equip -> { });
        recyclerEquipamentos.setLayoutManager(new LinearLayoutManager(this));
        recyclerEquipamentos.setAdapter(equipamentoAdapter);
        recyclerEquipamentos.setNestedScrollingEnabled(false);
    }

    private void carregarPiloto() {
        firebaseManager.obterPiloto(pilotoId).addOnSuccessListener(doc -> {
            if (doc == null || !doc.exists()) return;
            pilotoAtual = doc.toObject(Piloto.class);
            if (pilotoAtual == null) return;
            pilotoAtual.setId(doc.getId());
            preencherDadosPiloto(pilotoAtual);
        });
    }

    private void preencherDadosPiloto(Piloto piloto) {
        // Nome
        String nome = piloto.getNomeCompleto();
        tvNomePerfil.setText((nome != null && !nome.isEmpty()) ? nome : "Piloto");

        // Apelido
        String apelido = piloto.getApelido();
        if (apelido != null && !apelido.trim().isEmpty()) {
            tvApelidoPerfil.setText(apelido);
            tvApelidoPerfil.setVisibility(View.VISIBLE);
        } else {
            tvApelidoPerfil.setVisibility(View.GONE);
        }

        // Categoria
        String categoria = piloto.getCategoria();
        tvCategoriaPerfil.setText((categoria != null && !categoria.isEmpty()) ? categoria : "Piloto");

        // Idade
        int idade = piloto.getIdade();
        if (idade >= 0) {
            tvIdadePerfil.setText("• " + idade + " anos");
            tvIdadePerfil.setVisibility(View.VISIBLE);
        } else {
            tvIdadePerfil.setVisibility(View.GONE);
        }

        // Configurar número e cores no AvatarView (usado como fallback)
        String numero = piloto.getNumeroPiloto();
        avatarViewPerfil.setRiderNumber((numero != null && !numero.isEmpty()) ? numero : "#");
        try { if (piloto.getAvatarCor1() != null) avatarViewPerfil.setPrimaryColor(Color.parseColor(piloto.getAvatarCor1())); } catch (Exception ignored) {}
        try { if (piloto.getAvatarCor2() != null) avatarViewPerfil.setSecondaryColor(Color.parseColor(piloto.getAvatarCor2())); } catch (Exception ignored) {}

        // Preview gerado > fallback AvatarView
        if (!exibirPreviewSalvo(piloto)) {
            restaurarCustomizacoesAvatar(piloto);
        }

        // Stats
        tvCorridasStat.setText(piloto.getNumCorridas()  != null ? String.valueOf(piloto.getNumCorridas())  : "—");
        tvVitoriasStat.setText(piloto.getNumVitorias()  != null ? String.valueOf(piloto.getNumVitorias())  : "—");
        tvQuedasStat.setText(piloto.getNumQuedas()      != null ? String.valueOf(piloto.getNumQuedas())    : "—");
        String melhorTempo = piloto.getMelhorTempo();
        tvMelhorTempoStat.setText((melhorTempo != null && !melhorTempo.isEmpty()) ? melhorTempo : "—");
    }

    /** Carrega o bitmap PNG salvo localmente e o exibe. Retorna true se bem-sucedido. */
    private boolean exibirPreviewSalvo(Piloto piloto) {
        String path = piloto.getAvatarPreviewPath();
        if (path == null || path.isEmpty()) return false;
        File file = new File(path);
        if (!file.exists()) return false;
        try {
            android.graphics.Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bmp == null) return false;
            imgAvatarPreview.setImageBitmap(bmp);
            imgAvatarPreview.setVisibility(View.VISIBLE);
            frameAvatarFallback.setVisibility(View.GONE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Usado como fallback quando ainda não há bitmap gerado. */
    private void restaurarCustomizacoesAvatar(Piloto piloto) {
        imgAvatarPreview.setVisibility(View.GONE);
        frameAvatarFallback.setVisibility(View.VISIBLE);

        imgPerfilCapaceteOverlay.setVisibility(View.GONE);
        imgPerfilJaquetaOverlay.setVisibility(View.GONE);
        imgPerfilCalcaOverlay.setVisibility(View.GONE);
        imgPerfilBotasOverlay.setVisibility(View.GONE);

        // Capacete
        String cap = piloto.getCapacete();
        if ("agv_real".equals(cap)) {
            avatarViewPerfil.setShowHelmet(false);
            carregarOverlay(imgPerfilCapaceteOverlay, "agv_real_capacete");
        } else if ("custom".equals(cap) && piloto.getCapaceteImageUrl() != null) {
            avatarViewPerfil.setShowHelmet(false);
            Glide.with(this).load(Uri.parse(piloto.getCapaceteImageUrl())).into(imgPerfilCapaceteOverlay);
            imgPerfilCapaceteOverlay.setVisibility(View.VISIBLE);
        } else {
            avatarViewPerfil.setShowHelmet(true);
            if ("modular".equals(cap))     avatarViewPerfil.setHelmetStyle(1);
            else if ("offroad".equals(cap)) avatarViewPerfil.setHelmetStyle(2);
            else                            avatarViewPerfil.setHelmetStyle(0);
        }

        // Jaqueta
        String jaq = piloto.getJaqueta();
        if ("dainese_real".equals(jaq)) {
            avatarViewPerfil.setShowJaqueta(false);
            carregarOverlay(imgPerfilJaquetaOverlay, "jaqueta_dainese_real");
        } else if ("custom".equals(jaq) && piloto.getJacuetaImageUrl() != null) {
            avatarViewPerfil.setShowJaqueta(false);
            Glide.with(this).load(Uri.parse(piloto.getJacuetaImageUrl())).into(imgPerfilJaquetaOverlay);
            imgPerfilJaquetaOverlay.setVisibility(View.VISIBLE);
        } else {
            avatarViewPerfil.setShowJaqueta(true);
        }

        // Calça
        String cal = piloto.getCalca();
        if ("dainese_real".equals(cal)) {
            avatarViewPerfil.setShowCalca(false);
            carregarOverlay(imgPerfilCalcaOverlay, "calca_dainese_real");
        } else if ("custom".equals(cal) && piloto.getCalcaImageUrl() != null) {
            avatarViewPerfil.setShowCalca(false);
            Glide.with(this).load(Uri.parse(piloto.getCalcaImageUrl())).into(imgPerfilCalcaOverlay);
            imgPerfilCalcaOverlay.setVisibility(View.VISIBLE);
        } else {
            avatarViewPerfil.setShowCalca(true);
        }

        // Botas
        String bot = piloto.getBotas();
        if ("dainese_real".equals(bot)) {
            avatarViewPerfil.setShowBoots(false);
            carregarOverlay(imgPerfilBotasOverlay, "botas_dainese_real");
        } else if ("custom".equals(bot) && piloto.getBotasImageUrl() != null) {
            avatarViewPerfil.setShowBoots(false);
            Glide.with(this).load(Uri.parse(piloto.getBotasImageUrl())).into(imgPerfilBotasOverlay);
            imgPerfilBotasOverlay.setVisibility(View.VISIBLE);
        } else {
            avatarViewPerfil.setShowBoots(true);
        }
    }

    private void carregarOverlay(ImageView iv, String drawableName) {
        try {
            int id = getResources().getIdentifier(drawableName, "drawable", getPackageName());
            if (id != 0) { iv.setImageResource(id); iv.setVisibility(View.VISIBLE); }
        } catch (Exception ignored) {}
    }

    private void carregarEquipamentos() {
        firebaseManager.obterEquipamentosPiloto(pilotoId).addOnCompleteListener(task -> {
            equipamentos.clear();
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Equipamento equip = doc.toObject(Equipamento.class);
                    if (equip != null) { equip.setId(doc.getId()); equipamentos.add(equip); }
                }
            }
            equipamentoAdapter.notifyDataSetChanged();
            int total = equipamentos.size();
            if (total == 0) {
                recyclerEquipamentos.setVisibility(View.GONE);
                layoutSemEquipamentos.setVisibility(View.VISIBLE);
                tvQtdEquipamentos.setText("");
            } else {
                recyclerEquipamentos.setVisibility(View.VISIBLE);
                layoutSemEquipamentos.setVisibility(View.GONE);
                tvQtdEquipamentos.setText(total + (total == 1 ? " item" : " itens"));
            }
        });
    }
}
