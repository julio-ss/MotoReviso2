package br.jss.motoreviso.activities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Piloto;
import br.jss.motoreviso.utils.SystemBarHelper;
import br.jss.motoreviso.views.AvatarView;

public class AvatarPilotoActivity extends AppCompatActivity {

    private static final int[] CORES_PRIMARIAS = {
            0xFF00A8FF, 0xFFFF4D4D, 0xFF1FD68B, 0xFFFF8C00,
            0xFFFFD700, 0xFF7C4DFF, 0xFFF2F5F7, 0xFF37474F, 0xFF000000
    };
    private static final int[] CORES_SECUNDARIAS = {
            0xFFFFD700, 0xFFFFFFFF, 0xFFFF4D4D, 0xFF00A8FF,
            0xFF1FD68B, 0xFFFF8C00, 0xFF7C4DFF, 0xFF98A0A8, 0xFFFFFFFF
    };

    private String pilotoId;
    private Piloto pilotoAtual;
    private int selectedPrimary   = 0;
    private int selectedSecondary = 0;
    private int selectedHelmet    = 0;
    private int selectedJaqueta   = 0;
    private int selectedCalca     = 0;
    private int selectedBotas     = 0;

    // URIs das imagens customizadas
    private Uri selectedHelmetImageUri = null;
    private Uri selectedJaquetaImageUri = null;
    private Uri selectedCalcaImageUri = null;
    private Uri selectedBotasImageUri = null;

    // ActivityResultLaunchers para picker de imagens
    private ActivityResultLauncher<String> helmetImagePicker;
    private ActivityResultLauncher<String> jaquetaImagePicker;
    private ActivityResultLauncher<String> calcaImagePicker;
    private ActivityResultLauncher<String> botasImagePicker;

    // Views — Avatar + overlays (dentro do FrameLayout do avatar)
    private AvatarView     avatarView;
    private android.widget.ImageView imgAgvHelmetOverlay, imgDaineseJaquetaOverlay, imgDaineseCalcaOverlay, imgDaineseBotasOverlay;
    private android.widget.ImageView imgCustomHelmetOverlay, imgCustomJaquetaOverlay, imgCustomCalcaOverlay, imgCustomBotasOverlay;
    // Thumbnails dentro dos botões "+"
    private android.widget.ImageView imgThumbHelmet, imgThumbJaqueta, imgThumbCalca, imgThumbBotas;
    // Placeholders "+" dos botões
    private LinearLayout   layoutHelmetPlaceholder, layoutJaquetaPlaceholder, layoutCalcaPlaceholder, layoutBotasPlaceholder;
    // Outros
    private LinearLayout   layoutCoresPrimarias, layoutCoresSecundarias;
    private FrameLayout    btnHelmetIntegral, btnHelmetModular, btnHelmetOffroad, btnHelmetAgvReal, btnHelmetCustom;
    private FrameLayout    btnJaquetaRacing, btnJaquetaStreet, btnJaquetaTuring, btnJaquetaDaineseReal, btnJaquetaCustom;
    private FrameLayout    btnCalcaRacing, btnCalcaStreet, btnCalcaTuring, btnCalcaDaineseReal, btnCalcaCustom;
    private FrameLayout    btnBotasRacing, btnBotasStreet, btnBotasTuring, btnBotasDaineseReal, btnBotasCustom;
    private android.widget.ImageView imgHelmetAgv, imgJaquetaDainese, imgCalcaDainese, imgBotasDainese;
    private TextView       tvInfoNome, tvInfoNumero, tvInfoCategoria, tvInfoEquipe, tvInfoNac, tvInfoNasc;
    private MaterialButton btnSalvar, btnRedefinir;

    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_piloto);
        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        pilotoId = getIntent().getStringExtra("PILOTO_ID");
        if (pilotoId == null) { finish(); return; }

        firebaseManager = FirebaseManager.getInstance();

        // Criar ActivityResultLaunchers para picker de imagens
        criarImagemPickers();

        inicializarViews();
        construirSeletoresCores();
        configurarToggleEquipamentos();
        configurarSeletoresCapacete();
        configurarSeletoresJaqueta();
        configurarSeletoresCalca();
        configurarSeletoresBotas();
        carregarPiloto();

        btnSalvar.setOnClickListener(v -> salvarAvatar());
        btnRedefinir.setOnClickListener(v -> redefinirAvatar());
    }

    // ─── Criadores de Image Pickers ─────────────────────────────

    private void criarImagemPickers() {
        helmetImagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) onHelmetImageSelected(uri); }
        );
        jaquetaImagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) onJaquetaImageSelected(uri); }
        );
        calcaImagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) onCalcaImageSelected(uri); }
        );
        botasImagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) onBotasImageSelected(uri); }
        );
    }

    private void onHelmetImageSelected(Uri imageUri) {
        selectedHelmetImageUri = imageUri;
        selectedHelmet = 4;
        avatarView.setShowHelmet(false);
        ocultarTodasAsImagensCapacete();
        Glide.with(this).load(imageUri).into(imgCustomHelmetOverlay);
        imgCustomHelmetOverlay.setVisibility(View.VISIBLE);
        // Thumbnail no botão "+"
        Glide.with(this).load(imageUri).into(imgThumbHelmet);
        imgThumbHelmet.setVisibility(View.VISIBLE);
        layoutHelmetPlaceholder.setVisibility(View.GONE);
        atualizarBotaoCapaceteSelecionado();
    }

    private void onJaquetaImageSelected(Uri imageUri) {
        selectedJaquetaImageUri = imageUri;
        selectedJaqueta = 4;
        avatarView.setShowJaqueta(false);
        ocultarTodasAsImagensJaqueta();
        Glide.with(this).load(imageUri).into(imgCustomJaquetaOverlay);
        imgCustomJaquetaOverlay.setVisibility(View.VISIBLE);
        // Thumbnail no botão "+"
        Glide.with(this).load(imageUri).into(imgThumbJaqueta);
        imgThumbJaqueta.setVisibility(View.VISIBLE);
        layoutJaquetaPlaceholder.setVisibility(View.GONE);
        atualizarBotaoJaquetaSelecionada();
    }

    private void onCalcaImageSelected(Uri imageUri) {
        selectedCalcaImageUri = imageUri;
        selectedCalca = 4;
        avatarView.setShowCalca(false);
        ocultarTodasAsImagensCalca();
        Glide.with(this).load(imageUri).into(imgCustomCalcaOverlay);
        imgCustomCalcaOverlay.setVisibility(View.VISIBLE);
        // Thumbnail no botão "+"
        Glide.with(this).load(imageUri).into(imgThumbCalca);
        imgThumbCalca.setVisibility(View.VISIBLE);
        layoutCalcaPlaceholder.setVisibility(View.GONE);
        atualizarBotaoCalcaSelecionada();
    }

    private void onBotasImageSelected(Uri imageUri) {
        selectedBotasImageUri = imageUri;
        selectedBotas = 4;
        avatarView.setShowBoots(false);
        ocultarTodasAsImagensBotas();
        Glide.with(this).load(imageUri).into(imgCustomBotasOverlay);
        imgCustomBotasOverlay.setVisibility(View.VISIBLE);
        // Thumbnail no botão "+"
        Glide.with(this).load(imageUri).into(imgThumbBotas);
        imgThumbBotas.setVisibility(View.VISIBLE);
        layoutBotasPlaceholder.setVisibility(View.GONE);
        atualizarBotaoBotasSelecionadas();
    }

    private void inicializarViews() {
        avatarView                  = findViewById(R.id.avatar_view);
        imgAgvHelmetOverlay         = findViewById(R.id.img_agv_helmet_overlay);
        imgDaineseJaquetaOverlay    = findViewById(R.id.img_dainese_jaqueta_overlay);
        imgDaineseCalcaOverlay      = findViewById(R.id.img_dainese_calca_overlay);
        imgDaineseBotasOverlay      = findViewById(R.id.img_dainese_botas_overlay);
        imgCustomHelmetOverlay      = findViewById(R.id.img_custom_helmet_overlay);
        imgCustomJaquetaOverlay     = findViewById(R.id.img_custom_jaqueta_overlay);
        imgCustomCalcaOverlay       = findViewById(R.id.img_custom_calca_overlay);
        imgCustomBotasOverlay       = findViewById(R.id.img_custom_botas_overlay);
        // Thumbnails dos botões "+"
        imgThumbHelmet              = findViewById(R.id.img_thumb_helmet);
        imgThumbJaqueta             = findViewById(R.id.img_thumb_jaqueta);
        imgThumbCalca               = findViewById(R.id.img_thumb_calca);
        imgThumbBotas               = findViewById(R.id.img_thumb_botas);
        layoutHelmetPlaceholder     = findViewById(R.id.layout_helmet_placeholder);
        layoutJaquetaPlaceholder    = findViewById(R.id.layout_jaqueta_placeholder);
        layoutCalcaPlaceholder      = findViewById(R.id.layout_calca_placeholder);
        layoutBotasPlaceholder      = findViewById(R.id.layout_botas_placeholder);
        layoutCoresPrimarias        = findViewById(R.id.layout_cores_primarias);
        layoutCoresSecundarias      = findViewById(R.id.layout_cores_secundarias);
        btnHelmetIntegral           = findViewById(R.id.btn_helmet_integral);
        btnHelmetModular            = findViewById(R.id.btn_helmet_modular);
        btnHelmetOffroad            = findViewById(R.id.btn_helmet_offroad);
        btnHelmetAgvReal            = findViewById(R.id.btn_helmet_agv_real);
        btnHelmetCustom             = findViewById(R.id.btn_helmet_custom);
        btnJaquetaRacing            = findViewById(R.id.btn_jaqueta_racing);
        btnJaquetaStreet            = findViewById(R.id.btn_jaqueta_street);
        btnJaquetaTuring            = findViewById(R.id.btn_jaqueta_touring);
        btnJaquetaDaineseReal       = findViewById(R.id.btn_jaqueta_dainese_real);
        btnJaquetaCustom            = findViewById(R.id.btn_jaqueta_custom);
        btnCalcaRacing              = findViewById(R.id.btn_calca_racing);
        btnCalcaStreet              = findViewById(R.id.btn_calca_street);
        btnCalcaTuring              = findViewById(R.id.btn_calca_touring);
        btnCalcaDaineseReal         = findViewById(R.id.btn_calca_dainese_real);
        btnCalcaCustom              = findViewById(R.id.btn_calca_custom);
        btnBotasRacing              = findViewById(R.id.btn_botas_racing);
        btnBotasStreet              = findViewById(R.id.btn_botas_street);
        btnBotasTuring              = findViewById(R.id.btn_botas_touring);
        btnBotasDaineseReal         = findViewById(R.id.btn_botas_dainese_real);
        btnBotasCustom              = findViewById(R.id.btn_botas_custom);
        imgHelmetAgv                = findViewById(R.id.img_helmet_agv);
        imgJaquetaDainese           = findViewById(R.id.img_jaqueta_dainese);
        imgCalcaDainese             = findViewById(R.id.img_calca_dainese);
        imgBotasDainese             = findViewById(R.id.img_botas_dainese);
        tvInfoNome             = findViewById(R.id.tv_info_nome);
        tvInfoNumero           = findViewById(R.id.tv_info_numero);
        tvInfoCategoria        = findViewById(R.id.tv_info_categoria);
        tvInfoEquipe           = findViewById(R.id.tv_info_equipe);
        tvInfoNac              = findViewById(R.id.tv_info_nac);
        tvInfoNasc             = findViewById(R.id.tv_info_nasc);
        btnSalvar              = findViewById(R.id.btn_salvar_avatar);
        btnRedefinir           = findViewById(R.id.btn_redefinir_avatar);
    }

    // ─── Seletores de cor ────────────────────────────────────────

    private void construirSeletoresCores() {
        criarSwatches(layoutCoresPrimarias, CORES_PRIMARIAS, true);
        criarSwatches(layoutCoresSecundarias, CORES_SECUNDARIAS, false);
    }

    private void criarSwatches(LinearLayout container, int[] cores, boolean isPrimary) {
        container.removeAllViews();
        int sizePx = dpToPx(36);
        int marginPx = dpToPx(7);

        for (int i = 0; i < cores.length; i++) {
            final int index = i;
            final int cor = cores[i];

            // Container do swatch (inclui anel de seleção)
            FrameLayout frame = new FrameLayout(this);
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(sizePx + dpToPx(4), sizePx + dpToPx(4));
            frameParams.setMargins(0, 0, marginPx, 0);
            frame.setLayoutParams(frameParams);

            // Círculo colorido
            View circle = new View(this);
            FrameLayout.LayoutParams circleParams = new FrameLayout.LayoutParams(sizePx, sizePx);
            circleParams.gravity = android.view.Gravity.CENTER;
            circle.setLayoutParams(circleParams);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(cor);
            circle.setBackground(drawable);

            frame.addView(circle);
            container.addView(frame);

            // Marca a seleção inicial
            boolean selecionado = (isPrimary && index == selectedPrimary) ||
                                  (!isPrimary && index == selectedSecondary);
            atualizarSwatchSelecionado(frame, selecionado, cor);

            final FrameLayout finalFrame = frame;
            frame.setOnClickListener(v -> {
                if (isPrimary) {
                    selectedPrimary = index;
                    avatarView.setPrimaryColor(cor);
                    atualizarNumeroInfo(cor);
                } else {
                    selectedSecondary = index;
                    avatarView.setSecondaryColor(cor);
                }
                // Atualiza visual de todos os swatches do grupo
                for (int j = 0; j < container.getChildCount(); j++) {
                    FrameLayout f = (FrameLayout) container.getChildAt(j);
                    atualizarSwatchSelecionado(f, f == finalFrame, cores[j]);
                }
            });
        }
    }

    private void atualizarSwatchSelecionado(FrameLayout frame, boolean selected, int cor) {
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        if (selected) {
            ring.setColor(cor);
            ring.setStroke(dpToPx(3), Color.WHITE);
        } else {
            ring.setColor(cor);
            ring.setStroke(dpToPx(2), Color.argb(40, 255, 255, 255));
        }
        if (frame.getChildAt(0) != null) {
            frame.getChildAt(0).setBackground(ring);
        }
    }

    // ─── Seletores de capacete ───────────────────────────────────

    private void configurarSeletoresCapacete() {
        View.OnClickListener listener = v -> {
            int style;
            if (v == btnHelmetIntegral) style = 0;
            else if (v == btnHelmetModular) style = 1;
            else if (v == btnHelmetOffroad) style = 2;
            else style = 3;  // AGV Real

            selectedHelmet = style;
            if (style < 3) {
                // Estilo desenhado
                avatarView.setShowHelmet(true);
                avatarView.setHelmetStyle(style);
                ocultarTodasAsImagensCapacete();  // Ocultar Dainese + Custom anteriores
            } else if (style == 3) {
                // AGV Real - mostra apenas o capacete real como overlay
                avatarView.setShowHelmet(false);
                ocultarTodasAsImagensCapacete();  // Ocultar Custom anterior
                mostrarCapaceteAgvReal();
            } else {
                // Custom - já foi configurado em onHelmetImageSelected
            }
            atualizarBotaoCapaceteSelecionado();
        };
        btnHelmetIntegral.setOnClickListener(listener);
        btnHelmetModular.setOnClickListener(listener);
        btnHelmetOffroad.setOnClickListener(listener);
        btnHelmetAgvReal.setOnClickListener(listener);
        btnHelmetCustom.setOnClickListener(v -> helmetImagePicker.launch("image/*"));
        carregarImagemAgvReal();
        atualizarBotaoCapaceteSelecionado();
    }

    private void atualizarBotaoCapaceteSelecionado() {
        setFrameHighlight(btnHelmetIntegral, selectedHelmet == 0);
        setFrameHighlight(btnHelmetModular,  selectedHelmet == 1);
        setFrameHighlight(btnHelmetOffroad,  selectedHelmet == 2);
        setFrameHighlight(btnHelmetAgvReal,  selectedHelmet == 3);
    }

    private void setFrameHighlight(FrameLayout frame, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(10));
        if (selected) {
            bg.setColor(Color.argb(40, Color.red(CORES_PRIMARIAS[selectedPrimary]),
                    Color.green(CORES_PRIMARIAS[selectedPrimary]),
                    Color.blue(CORES_PRIMARIAS[selectedPrimary])));
            bg.setStroke(dpToPx(2), CORES_PRIMARIAS[selectedPrimary]);
        } else {
            bg.setColor(ContextCompat.getColor(this, R.color.surface_variant));
            bg.setStroke(dpToPx(1), ContextCompat.getColor(this, R.color.divider_strong));
        }
        frame.setBackground(bg);
    }

    // ─── Seletores de jaqueta ───────────────────────────────────

    private void configurarSeletoresJaqueta() {
        View.OnClickListener listener = v -> {
            int style;
            if (v == btnJaquetaRacing) style = 0;
            else if (v == btnJaquetaStreet) style = 1;
            else if (v == btnJaquetaTuring) style = 2;
            else style = 3;  // Dainese Real

            selectedJaqueta = style;
            if (style < 3) {
                // Estilo desenhado
                avatarView.setShowJaqueta(true);
                ocultarTodasAsImagensJaqueta();  // Ocultar Dainese + Custom anteriores
            } else if (style == 3) {
                // Dainese Real - mostra jaqueta real como overlay
                avatarView.setShowJaqueta(false);
                ocultarTodasAsImagensJaqueta();  // Ocultar Custom anterior
                mostrarJaquetaDaineseReal();
            } else {
                // Custom - já foi configurado em onJaquetaImageSelected
            }
            atualizarBotaoJaquetaSelecionada();
        };
        btnJaquetaRacing.setOnClickListener(listener);
        btnJaquetaStreet.setOnClickListener(listener);
        btnJaquetaTuring.setOnClickListener(listener);
        btnJaquetaDaineseReal.setOnClickListener(listener);
        btnJaquetaCustom.setOnClickListener(v -> jaquetaImagePicker.launch("image/*"));
        carregarImagemDaineseReal();
        atualizarBotaoJaquetaSelecionada();
    }

    private void atualizarBotaoJaquetaSelecionada() {
        setFrameHighlight(btnJaquetaRacing,      selectedJaqueta == 0);
        setFrameHighlight(btnJaquetaStreet,      selectedJaqueta == 1);
        setFrameHighlight(btnJaquetaTuring,      selectedJaqueta == 2);
        setFrameHighlight(btnJaquetaDaineseReal, selectedJaqueta == 3);
    }

    // ─── Exibição da Jaqueta Dainese Real ────────────────────────

    private void mostrarJaquetaDaineseReal() {
        avatarView.setShowJaqueta(false);  // Ocultar desenho da jaqueta
        imgDaineseJaquetaOverlay.setVisibility(View.VISIBLE);
    }

    private void ocultarJaquetaDaineseReal() {
        imgDaineseJaquetaOverlay.setVisibility(View.GONE);
    }

    // ─── Carregamento de imagem Dainese ─────────────────────────

    private void carregarImagemDaineseReal() {
        try {
            int drawableId = getResources().getIdentifier("jaqueta_dainese_real", "drawable", getPackageName());
            if (drawableId != 0) {
                imgDaineseJaquetaOverlay.setImageDrawable(ContextCompat.getDrawable(this, drawableId));
                imgJaquetaDainese.setImageDrawable(ContextCompat.getDrawable(this, drawableId));
            } else {
                imgDaineseJaquetaOverlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jaqueta_racing));
                imgJaquetaDainese.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jaqueta_racing));
            }
        } catch (Exception e) {
            imgDaineseJaquetaOverlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jaqueta_racing));
            imgJaquetaDainese.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_jaqueta_racing));
        }
    }

    // ─── Toggles de equipamentos ─────────────────────────────────

    private void configurarToggleEquipamentos() {
    }

    // ─── Carregar dados do piloto ────────────────────────────────

    private void carregarPiloto() {
        firebaseManager.obterPiloto(pilotoId).addOnSuccessListener(doc -> {
            if (doc == null || !doc.exists()) return;
            pilotoAtual = doc.toObject(Piloto.class);
            if (pilotoAtual == null) return;
            pilotoAtual.setId(doc.getId());
            aplicarDadosPiloto(pilotoAtual);
        });
    }

    private void aplicarDadosPiloto(Piloto piloto) {
        // Número
        String numero = piloto.getNumeroPiloto();
        avatarView.setRiderNumber(numero != null ? numero : "");
        tvInfoNumero.setText((numero != null && !numero.isEmpty()) ? numero : "—");

        // Cor do número (secondary)
        avatarView.post(() -> tvInfoNumero.setTextColor(avatarView.getSecondaryColor()));

        // Cor primária salva
        if (piloto.getAvatarCor1() != null) {
            try {
                int corSalva = Color.parseColor(piloto.getAvatarCor1());
                avatarView.setPrimaryColor(corSalva);
                selecionarCorMaisProxima(corSalva, true);
            } catch (Exception ignored) {}
        }

        // Cor secundária salva
        if (piloto.getAvatarCor2() != null) {
            try {
                int corSalva = Color.parseColor(piloto.getAvatarCor2());
                avatarView.setSecondaryColor(corSalva);
                selecionarCorMaisProxima(corSalva, false);
                tvInfoNumero.setTextColor(corSalva);
            } catch (Exception ignored) {}
        }

        // Estilo do capacete
        if (piloto.getCapacete() != null) {
            switch (piloto.getCapacete()) {
                case "modular":  selectedHelmet = 1; break;
                case "offroad":  selectedHelmet = 2; break;
                case "agv_real": selectedHelmet = 3; break;
                case "custom":   selectedHelmet = 4; break;
                default:         selectedHelmet = 0; break;
            }
            if (selectedHelmet < 3) {
                avatarView.setHelmetStyle(selectedHelmet);
                avatarView.setShowHelmet(true);
                ocultarTodasAsImagensCapacete();
            } else if (selectedHelmet == 3) {
                // AGV Real - mostra capacete real como overlay
                avatarView.setShowHelmet(false);
                ocultarTodasAsImagensCapacete();
                mostrarCapaceteAgvReal();
            } else if (selectedHelmet == 4) {
                avatarView.setShowHelmet(false);
                if (piloto.getCapaceteImageUrl() != null && !piloto.getCapaceteImageUrl().isEmpty()) {
                    selectedHelmetImageUri = Uri.parse(piloto.getCapaceteImageUrl());
                    ocultarTodasAsImagensCapacete();
                    Glide.with(this).load(selectedHelmetImageUri).into(imgCustomHelmetOverlay);
                    imgCustomHelmetOverlay.setVisibility(View.VISIBLE);
                    Glide.with(this).load(selectedHelmetImageUri).into(imgThumbHelmet);
                    imgThumbHelmet.setVisibility(View.VISIBLE);
                    layoutHelmetPlaceholder.setVisibility(View.GONE);
                }
            }
            atualizarBotaoCapaceteSelecionado();
        } else {
            ocultarTodasAsImagensCapacete();  // Default: mostrar avatar com capacete desenhado
        }

        // Estilo da jaqueta
        if (piloto.getJaqueta() != null) {
            switch (piloto.getJaqueta()) {
                case "street":       selectedJaqueta = 1; break;
                case "touring":      selectedJaqueta = 2; break;
                case "dainese_real": selectedJaqueta = 3; break;
                case "custom":       selectedJaqueta = 4; break;
                default:             selectedJaqueta = 0; break;
            }
            if (selectedJaqueta < 3) {
                avatarView.setShowJaqueta(true);
                ocultarTodasAsImagensJaqueta();
            } else if (selectedJaqueta == 3) {
                // Dainese Real - mostra jaqueta real como overlay
                avatarView.setShowJaqueta(false);
                ocultarTodasAsImagensJaqueta();
                mostrarJaquetaDaineseReal();
            } else if (selectedJaqueta == 4) {
                avatarView.setShowJaqueta(false);
                if (piloto.getJacuetaImageUrl() != null && !piloto.getJacuetaImageUrl().isEmpty()) {
                    selectedJaquetaImageUri = Uri.parse(piloto.getJacuetaImageUrl());
                    ocultarTodasAsImagensJaqueta();
                    Glide.with(this).load(selectedJaquetaImageUri).into(imgCustomJaquetaOverlay);
                    imgCustomJaquetaOverlay.setVisibility(View.VISIBLE);
                    Glide.with(this).load(selectedJaquetaImageUri).into(imgThumbJaqueta);
                    imgThumbJaqueta.setVisibility(View.VISIBLE);
                    layoutJaquetaPlaceholder.setVisibility(View.GONE);
                }
            }
            atualizarBotaoJaquetaSelecionada();
        } else {
            ocultarTodasAsImagensJaqueta();  // Default: sem sobreposição
        }

        // Estilo da calça
        if (piloto.getCalca() != null) {
            switch (piloto.getCalca()) {
                case "street":       selectedCalca = 1; break;
                case "touring":      selectedCalca = 2; break;
                case "dainese_real": selectedCalca = 3; break;
                case "custom":       selectedCalca = 4; break;
                default:             selectedCalca = 0; break;
            }
            if (selectedCalca < 3) {
                avatarView.setShowCalca(true);
                ocultarTodasAsImagensCalca();
            } else if (selectedCalca == 3) {
                // Dainese Real - mostra calça real como overlay
                avatarView.setShowCalca(false);
                ocultarTodasAsImagensCalca();
                mostrarCalcaDaineseReal();
            } else if (selectedCalca == 4) {
                avatarView.setShowCalca(false);
                if (piloto.getCalcaImageUrl() != null && !piloto.getCalcaImageUrl().isEmpty()) {
                    selectedCalcaImageUri = Uri.parse(piloto.getCalcaImageUrl());
                    ocultarTodasAsImagensCalca();
                    Glide.with(this).load(selectedCalcaImageUri).into(imgCustomCalcaOverlay);
                    imgCustomCalcaOverlay.setVisibility(View.VISIBLE);
                    Glide.with(this).load(selectedCalcaImageUri).into(imgThumbCalca);
                    imgThumbCalca.setVisibility(View.VISIBLE);
                    layoutCalcaPlaceholder.setVisibility(View.GONE);
                }
            }
            atualizarBotaoCalcaSelecionada();
        } else {
            ocultarTodasAsImagensCalca();  // Default: sem sobreposição
        }

        // Estilo das botas
        if (piloto.getBotas() != null) {
            switch (piloto.getBotas()) {
                case "street":       selectedBotas = 1; break;
                case "touring":      selectedBotas = 2; break;
                case "dainese_real": selectedBotas = 3; break;
                case "custom":       selectedBotas = 4; break;
                default:             selectedBotas = 0; break;
            }
            if (selectedBotas < 3) {
                avatarView.setShowBoots(true);
                ocultarTodasAsImagensBotas();
            } else if (selectedBotas == 3) {
                // Dainese Real - mostra botas real como overlay
                avatarView.setShowBoots(false);
                ocultarTodasAsImagensBotas();
                mostrarBotasDaineseReal();
            } else if (selectedBotas == 4) {
                avatarView.setShowBoots(false);
                if (piloto.getBotasImageUrl() != null && !piloto.getBotasImageUrl().isEmpty()) {
                    selectedBotasImageUri = Uri.parse(piloto.getBotasImageUrl());
                    ocultarTodasAsImagensBotas();
                    Glide.with(this).load(selectedBotasImageUri).into(imgCustomBotasOverlay);
                    imgCustomBotasOverlay.setVisibility(View.VISIBLE);
                    Glide.with(this).load(selectedBotasImageUri).into(imgThumbBotas);
                    imgThumbBotas.setVisibility(View.VISIBLE);
                    layoutBotasPlaceholder.setVisibility(View.GONE);
                }
            }
            atualizarBotaoBotasSelecionadas();
        } else {
            ocultarTodasAsImagensBotas();  // Default: sem sobreposição
        }

        // Informações de texto
        tvInfoNome.setText(str(piloto.getNomeCompleto(), "—"));
        tvInfoCategoria.setText(str(piloto.getCategoria(), "—"));
        tvInfoEquipe.setText(str(piloto.getEquipe(), "—"));
        tvInfoNac.setText(str(piloto.getNacionalidade(), "—"));

        if (piloto.getDataNascimento() != null) {
            tvInfoNasc.setText(new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"))
                    .format(new Date(piloto.getDataNascimento())));
        } else {
            tvInfoNasc.setText("—");
        }

        // Atualizar seletores de cor visuais
        construirSeletoresCores();
    }

    private void selecionarCorMaisProxima(int cor, boolean isPrimary) {
        int[] palette = isPrimary ? CORES_PRIMARIAS : CORES_SECUNDARIAS;
        int melhorIndice = 0;
        double melhorDist = Double.MAX_VALUE;
        for (int i = 0; i < palette.length; i++) {
            double dist = distanciaCores(cor, palette[i]);
            if (dist < melhorDist) { melhorDist = dist; melhorIndice = i; }
        }
        if (isPrimary) selectedPrimary = melhorIndice;
        else selectedSecondary = melhorIndice;
    }

    private double distanciaCores(int c1, int c2) {
        double dr = Color.red(c1) - Color.red(c2);
        double dg = Color.green(c1) - Color.green(c2);
        double db = Color.blue(c1) - Color.blue(c2);
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    // ─── Exibição do Capacete AGV Real ──────────────────────────

    private void mostrarCapaceteAgvReal() {
        avatarView.setShowHelmet(false);  // Ocultar desenho do capacete
        imgAgvHelmetOverlay.setVisibility(View.VISIBLE);
    }

    private void ocultarCapaceteAgvReal() {
        imgAgvHelmetOverlay.setVisibility(View.GONE);
        avatarView.setShowHelmet(true);  // Re-ativa capacete desenhado
    }

    // ─── Carregamento de imagens ─────────────────────────────────

    private void carregarImagemAgvReal() {
        // Tenta carregar a imagem real do capacete AGV
        try {
            int drawableId = getResources().getIdentifier("helmet_agv_real", "drawable", getPackageName());
            if (drawableId != 0) {
                // Imagem real encontrada
                imgAgvHelmetOverlay.setImageDrawable(ContextCompat.getDrawable(this, drawableId));
                imgHelmetAgv.setImageDrawable(ContextCompat.getDrawable(this, drawableId));
            } else {
                // Fallback: usar ícone padrão integral
                imgAgvHelmetOverlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_helmet_integral));
                imgHelmetAgv.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_helmet_integral));
            }
        } catch (Exception e) {
            // Se falhar, usar ícone padrão
            imgAgvHelmetOverlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_helmet_integral));
            imgHelmetAgv.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_helmet_integral));
        }
    }

    // ─── Seletores de botas ─────────────────────────────────────

    private void configurarSeletoresBotas() {
        View.OnClickListener listener = v -> {
            int style;
            if (v == btnBotasRacing) style = 0;
            else if (v == btnBotasStreet) style = 1;
            else if (v == btnBotasTuring) style = 2;
            else style = 3;  // Dainese Real

            selectedBotas = style;
            if (style < 3) {
                // Estilo desenhado
                avatarView.setShowBoots(true);
                ocultarTodasAsImagensBotas();  // Ocultar Dainese + Custom anteriores
            } else if (style == 3) {
                // Dainese Real - mostra botas real como overlay
                avatarView.setShowBoots(false);
                ocultarTodasAsImagensBotas();  // Ocultar Custom anterior
                mostrarBotasDaineseReal();
            } else {
                // Custom - já foi configurado em onBotasImageSelected
            }
            atualizarBotaoBotasSelecionadas();
        };
        btnBotasRacing.setOnClickListener(listener);
        btnBotasStreet.setOnClickListener(listener);
        btnBotasTuring.setOnClickListener(listener);
        btnBotasDaineseReal.setOnClickListener(listener);
        btnBotasCustom.setOnClickListener(v -> botasImagePicker.launch("image/*"));
        carregarImagemBotasDaineseReal();
        atualizarBotaoBotasSelecionadas();
    }

    private void atualizarBotaoBotasSelecionadas() {
        setFrameHighlight(btnBotasRacing,      selectedBotas == 0);
        setFrameHighlight(btnBotasStreet,      selectedBotas == 1);
        setFrameHighlight(btnBotasTuring,      selectedBotas == 2);
        setFrameHighlight(btnBotasDaineseReal, selectedBotas == 3);
    }

    // ─── Exibição das Botas Dainese Real ────────────────────────

    private void mostrarBotasDaineseReal() {
        avatarView.setShowBoots(false);  // Ocultar desenho das botas
        imgDaineseBotasOverlay.setVisibility(View.VISIBLE);
    }

    private void ocultarBotasDaineseReal() {
        imgDaineseBotasOverlay.setVisibility(View.GONE);
    }

    // ─── Carregamento de imagem Botas Dainese ───────────────────

    private void carregarImagemBotasDaineseReal() {
        try {
            int drawableId = getResources().getIdentifier("botas_dainese_real", "drawable", getPackageName());
            if (drawableId != 0) {
                imgDaineseBotasOverlay.setImageDrawable(ContextCompat.getDrawable(this, drawableId));
                imgBotasDainese.setImageDrawable(ContextCompat.getDrawable(this, drawableId));
            } else {
                imgDaineseBotasOverlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_botas_racing));
                imgBotasDainese.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_botas_racing));
            }
        } catch (Exception e) {
            imgDaineseBotasOverlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_botas_racing));
            imgBotasDainese.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_botas_racing));
        }
    }

    // ─── Seletores de calça ─────────────────────────────────────

    private void configurarSeletoresCalca() {
        View.OnClickListener listener = v -> {
            int style;
            if (v == btnCalcaRacing) style = 0;
            else if (v == btnCalcaStreet) style = 1;
            else if (v == btnCalcaTuring) style = 2;
            else style = 3;  // Dainese Real

            selectedCalca = style;
            if (style < 3) {
                // Estilo desenhado
                avatarView.setShowCalca(true);
                ocultarTodasAsImagensCalca();  // Ocultar Dainese + Custom anteriores
            } else if (style == 3) {
                // Dainese Real - mostra calça real como overlay
                avatarView.setShowCalca(false);
                ocultarTodasAsImagensCalca();  // Ocultar Custom anterior
                mostrarCalcaDaineseReal();
            } else {
                // Custom - já foi configurado em onCalcaImageSelected
            }
            atualizarBotaoCalcaSelecionada();
        };
        btnCalcaRacing.setOnClickListener(listener);
        btnCalcaStreet.setOnClickListener(listener);
        btnCalcaTuring.setOnClickListener(listener);
        btnCalcaDaineseReal.setOnClickListener(listener);
        btnCalcaCustom.setOnClickListener(v -> calcaImagePicker.launch("image/*"));
        carregarImagemCalcaDaineseReal();
        atualizarBotaoCalcaSelecionada();
    }

    private void atualizarBotaoCalcaSelecionada() {
        setFrameHighlight(btnCalcaRacing,      selectedCalca == 0);
        setFrameHighlight(btnCalcaStreet,      selectedCalca == 1);
        setFrameHighlight(btnCalcaTuring,      selectedCalca == 2);
        setFrameHighlight(btnCalcaDaineseReal, selectedCalca == 3);
    }

    // ─── Exibição da Calça Dainese Real ─────────────────────────

    private void mostrarCalcaDaineseReal() {
        avatarView.setShowCalca(false);  // Ocultar desenho da calça
        imgDaineseCalcaOverlay.setVisibility(View.VISIBLE);
    }

    private void ocultarCalcaDaineseReal() {
        imgDaineseCalcaOverlay.setVisibility(View.GONE);
    }

    // ─── Carregamento de imagem Calça Dainese ────────────────────

    private void carregarImagemCalcaDaineseReal() {
        try {
            int drawableId = getResources().getIdentifier("calca_dainese_real", "drawable", getPackageName());
            if (drawableId != 0) {
                imgDaineseCalcaOverlay.setImageDrawable(ContextCompat.getDrawable(this, drawableId));
                imgCalcaDainese.setImageDrawable(ContextCompat.getDrawable(this, drawableId));
            } else {
                imgDaineseCalcaOverlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_calca_racing));
                imgCalcaDainese.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_calca_racing));
            }
        } catch (Exception e) {
            imgDaineseCalcaOverlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_calca_racing));
            imgCalcaDainese.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_calca_racing));
        }
    }

    // ─── Salvar / Redefinir ──────────────────────────────────────

    private void salvarAvatar() {
        if (pilotoAtual == null) return;

        // Formatar cores como hex
        int primary   = avatarView.getPrimaryColor();
        int secondary = avatarView.getSecondaryColor();
        pilotoAtual.setAvatarCor1(String.format("#%06X", (0xFFFFFF & primary)));
        pilotoAtual.setAvatarCor2(String.format("#%06X", (0xFFFFFF & secondary)));

        // Estilo do capacete
        switch (selectedHelmet) {
            case 1: pilotoAtual.setCapacete("modular"); pilotoAtual.setCapaceteImageUrl(null); break;
            case 2: pilotoAtual.setCapacete("offroad"); pilotoAtual.setCapaceteImageUrl(null); break;
            case 3: pilotoAtual.setCapacete("agv_real"); pilotoAtual.setCapaceteImageUrl(null); break;
            case 4: pilotoAtual.setCapacete("custom"); pilotoAtual.setCapaceteImageUrl(selectedHelmetImageUri != null ? selectedHelmetImageUri.toString() : null); break;
            default: pilotoAtual.setCapacete("integral"); pilotoAtual.setCapaceteImageUrl(null); break;
        }

        // Estilo da jaqueta
        switch (selectedJaqueta) {
            case 1: pilotoAtual.setJaqueta("street"); pilotoAtual.setJacuetaImageUrl(null); break;
            case 2: pilotoAtual.setJaqueta("touring"); pilotoAtual.setJacuetaImageUrl(null); break;
            case 3: pilotoAtual.setJaqueta("dainese_real"); pilotoAtual.setJacuetaImageUrl(null); break;
            case 4: pilotoAtual.setJaqueta("custom"); pilotoAtual.setJacuetaImageUrl(selectedJaquetaImageUri != null ? selectedJaquetaImageUri.toString() : null); break;
            default: pilotoAtual.setJaqueta("racing"); pilotoAtual.setJacuetaImageUrl(null); break;
        }

        // Estilo da calça
        switch (selectedCalca) {
            case 1: pilotoAtual.setCalca("street"); pilotoAtual.setCalcaImageUrl(null); break;
            case 2: pilotoAtual.setCalca("touring"); pilotoAtual.setCalcaImageUrl(null); break;
            case 3: pilotoAtual.setCalca("dainese_real"); pilotoAtual.setCalcaImageUrl(null); break;
            case 4: pilotoAtual.setCalca("custom"); pilotoAtual.setCalcaImageUrl(selectedCalcaImageUri != null ? selectedCalcaImageUri.toString() : null); break;
            default: pilotoAtual.setCalca("racing"); pilotoAtual.setCalcaImageUrl(null); break;
        }

        // Estilo das botas
        switch (selectedBotas) {
            case 1: pilotoAtual.setBotas("street"); pilotoAtual.setBotasImageUrl(null); break;
            case 2: pilotoAtual.setBotas("touring"); pilotoAtual.setBotasImageUrl(null); break;
            case 3: pilotoAtual.setBotas("dainese_real"); pilotoAtual.setBotasImageUrl(null); break;
            case 4: pilotoAtual.setBotas("custom"); pilotoAtual.setBotasImageUrl(selectedBotasImageUri != null ? selectedBotasImageUri.toString() : null); break;
            default: pilotoAtual.setBotas("racing"); pilotoAtual.setBotasImageUrl(null); break;
        }

        // Gerar e salvar o bitmap do avatar antes de persistir no Firebase
        String previewPath = gerarESalvarBitmapAvatar();
        if (previewPath != null) {
            pilotoAtual.setAvatarPreviewPath(previewPath);
        }

        firebaseManager.atualizarPiloto(pilotoAtual)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Avatar salvo com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void redefinirAvatar() {
        selectedPrimary   = 0;
        selectedSecondary = 0;
        selectedHelmet    = 0;
        selectedJaqueta   = 0;
        selectedCalca     = 0;
        selectedBotas     = 0;
        selectedHelmetImageUri = null;
        selectedJaquetaImageUri = null;
        selectedCalcaImageUri = null;
        selectedBotasImageUri = null;
        ocultarTodasAsImagensCapacete();
        ocultarTodasAsImagensJaqueta();
        ocultarTodasAsImagensCalca();
        ocultarTodasAsImagensBotas();
        // Resetar thumbnails dos botões "+"
        imgThumbHelmet.setVisibility(View.GONE);  layoutHelmetPlaceholder.setVisibility(View.VISIBLE);
        imgThumbJaqueta.setVisibility(View.GONE); layoutJaquetaPlaceholder.setVisibility(View.VISIBLE);
        imgThumbCalca.setVisibility(View.GONE);   layoutCalcaPlaceholder.setVisibility(View.VISIBLE);
        imgThumbBotas.setVisibility(View.GONE);   layoutBotasPlaceholder.setVisibility(View.VISIBLE);
        avatarView.setPrimaryColor(CORES_PRIMARIAS[0]);
        avatarView.setSecondaryColor(CORES_SECUNDARIAS[0]);
        avatarView.setHelmetStyle(0);
        avatarView.setShowHelmet(true);
        avatarView.setShowBalaclava(false);
        avatarView.setShowGloves(true);
        avatarView.setShowBoots(true);
        avatarView.setShowSpineProtector(false);
        avatarView.setShowAirbagVest(false);
        avatarView.setShowGlasses(false);

        construirSeletoresCores();
        atualizarBotaoCapaceteSelecionado();
        atualizarBotaoJaquetaSelecionada();
        atualizarBotaoCalcaSelecionada();
        atualizarBotaoBotasSelecionadas();
        tvInfoNumero.setTextColor(CORES_SECUNDARIAS[0]);
    }

    private void atualizarNumeroInfo(int cor) {
        tvInfoNumero.setTextColor(cor);
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private String str(String val, String fallback) {
        return (val != null && !val.trim().isEmpty()) ? val : fallback;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ─── Ocultar TODAS as imagens de um equipamento ────────────────

    /** Oculta TODAS as imagens do capacete (AGV Real + Custom) */
    private void ocultarTodasAsImagensCapacete() {
        imgAgvHelmetOverlay.setVisibility(View.GONE);
        imgCustomHelmetOverlay.setVisibility(View.GONE);
    }

    /** Oculta TODAS as imagens da jaqueta (Dainese Real + Custom) */
    private void ocultarTodasAsImagensJaqueta() {
        imgDaineseJaquetaOverlay.setVisibility(View.GONE);
        imgCustomJaquetaOverlay.setVisibility(View.GONE);
    }

    /** Oculta TODAS as imagens da calça (Dainese Real + Custom) */
    private void ocultarTodasAsImagensCalca() {
        imgDaineseCalcaOverlay.setVisibility(View.GONE);
        imgCustomCalcaOverlay.setVisibility(View.GONE);
    }

    /** Oculta TODAS as imagens das botas (Dainese Real + Custom) */
    private void ocultarTodasAsImagensBotas() {
        imgDaineseBotasOverlay.setVisibility(View.GONE);
        imgCustomBotasOverlay.setVisibility(View.GONE);
    }

    // ─── Geração do Bitmap do Avatar ─────────────────────────────

    /**
     * Gera um bitmap com seções verticais para cada equipamento do piloto.
     * Cada seção mostra a imagem escolhida (predefinida ou da galeria).
     * Salva como PNG no diretório interno do app.
     * @return caminho absoluto do arquivo salvo, ou null em caso de erro
     */
    private String gerarESalvarBitmapAvatar() {
        try {
            final int WIDTH         = 400;
            final int SECTION_H     = 300; // altura de cada seção de equipamento
            final int DIVIDER_H     = 3;
            final int SECTIONS      = 4;   // capacete, jaqueta, calça, botas
            final int TOTAL_H       = SECTIONS * SECTION_H + (SECTIONS - 1) * DIVIDER_H;
            final int BG_COLOR      = 0xFF12121C;
            final int DIVIDER_COLOR = 0xFF2A2A3E;

            Bitmap result = Bitmap.createBitmap(WIDTH, TOTAL_H, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            canvas.drawColor(BG_COLOR);

            android.graphics.Paint divPaint = new android.graphics.Paint();
            divPaint.setColor(DIVIDER_COLOR);

            // --- Capacete ---
            Bitmap capBmp = obterBitmapEquipamento("capacete");
            desenharSecao(canvas, capBmp, 0, SECTION_H, WIDTH, BG_COLOR);
            if (capBmp != null) capBmp.recycle();

            int y = SECTION_H;
            canvas.drawRect(0, y, WIDTH, y + DIVIDER_H, divPaint);
            y += DIVIDER_H;

            // --- Jaqueta ---
            Bitmap jaqBmp = obterBitmapEquipamento("jaqueta");
            desenharSecao(canvas, jaqBmp, y, SECTION_H, WIDTH, BG_COLOR);
            if (jaqBmp != null) jaqBmp.recycle();

            y += SECTION_H;
            canvas.drawRect(0, y, WIDTH, y + DIVIDER_H, divPaint);
            y += DIVIDER_H;

            // --- Calça ---
            Bitmap calBmp = obterBitmapEquipamento("calca");
            desenharSecao(canvas, calBmp, y, SECTION_H, WIDTH, BG_COLOR);
            if (calBmp != null) calBmp.recycle();

            y += SECTION_H;
            canvas.drawRect(0, y, WIDTH, y + DIVIDER_H, divPaint);
            y += DIVIDER_H;

            // --- Botas ---
            Bitmap botBmp = obterBitmapEquipamento("botas");
            desenharSecao(canvas, botBmp, y, SECTION_H, WIDTH, BG_COLOR);
            if (botBmp != null) botBmp.recycle();

            // Salvar
            File dir = new File(getFilesDir(), "avatars");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "avatar_" + pilotoAtual.getId() + ".png");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                result.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
            }
            result.recycle();
            return file.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtém o Bitmap correspondente ao equipamento escolhido.
     * @param tipo "capacete", "jaqueta", "calca" ou "botas"
     */
    private Bitmap obterBitmapEquipamento(String tipo) {
        try {
            switch (tipo) {
                case "capacete": return bitmapParaCapacete();
                case "jaqueta":  return bitmapParaJaqueta();
                case "calca":    return bitmapParaCalca();
                case "botas":    return bitmapParaBotas();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private Bitmap bitmapParaCapacete() throws Exception {
        switch (selectedHelmet) {
            // Case 3: imagem predefinida AGV — pegar do ImageView overlay (já carregado)
            case 3:  return bitmapDeImageView(imgAgvHelmetOverlay);
            case 4:  return selectedHelmetImageUri != null ? bitmapDeUri(selectedHelmetImageUri) : null;
            case 1:  return bitmapDeDrawable(R.drawable.ic_helmet_modular);
            case 2:  return bitmapDeDrawable(R.drawable.ic_helmet_offroad);
            default: return bitmapDeDrawable(R.drawable.ic_helmet_integral);
        }
    }

    private Bitmap bitmapParaJaqueta() throws Exception {
        switch (selectedJaqueta) {
            // Case 3: imagem predefinida Dainese — pegar do ImageView overlay (já carregado)
            case 3:  return bitmapDeImageView(imgDaineseJaquetaOverlay);
            case 4:  return selectedJaquetaImageUri != null ? bitmapDeUri(selectedJaquetaImageUri) : null;
            case 1:  return bitmapDeDrawable(R.drawable.ic_jaqueta_street);
            case 2:  return bitmapDeDrawable(R.drawable.ic_jaqueta_touring);
            default: return bitmapDeDrawable(R.drawable.ic_jaqueta_racing);
        }
    }

    private Bitmap bitmapParaCalca() throws Exception {
        switch (selectedCalca) {
            case 3:  return bitmapDeImageView(imgDaineseCalcaOverlay);
            case 4:  return selectedCalcaImageUri != null ? bitmapDeUri(selectedCalcaImageUri) : null;
            case 1:  return bitmapDeDrawable(R.drawable.ic_calca_street);
            case 2:  return bitmapDeDrawable(R.drawable.ic_calca_touring);
            default: return bitmapDeDrawable(R.drawable.ic_calca_racing);
        }
    }

    private Bitmap bitmapParaBotas() throws Exception {
        switch (selectedBotas) {
            case 3:  return bitmapDeImageView(imgDaineseBotasOverlay);
            case 4:  return selectedBotasImageUri != null ? bitmapDeUri(selectedBotasImageUri) : null;
            case 1:  return bitmapDeDrawable(R.drawable.ic_botas_street);
            case 2:  return bitmapDeDrawable(R.drawable.ic_botas_touring);
            default: return bitmapDeDrawable(R.drawable.ic_botas_racing);
        }
    }

    /**
     * Extrai o Bitmap de um ImageView que já tem imagem carregada (predefinida ou Glide).
     * Renderiza o drawable do ImageView em um novo Bitmap.
     */
    private Bitmap bitmapDeImageView(android.widget.ImageView iv) {
        if (iv == null) return null;
        android.graphics.drawable.Drawable d = iv.getDrawable();
        if (d == null) return null;
        // Usar tamanho real do drawable; fallback 400x400 para bitmaps sem intrínseco
        int w = d.getIntrinsicWidth()  > 0 ? d.getIntrinsicWidth()  : 400;
        int h = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : 400;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return bmp;
    }

    /** Converte um drawable resource em Bitmap */
    private Bitmap bitmapDeDrawable(int resId) {
        android.graphics.drawable.Drawable d = ContextCompat.getDrawable(this, resId);
        if (d == null) return null;
        int w = Math.max(d.getIntrinsicWidth(), 200);
        int h = Math.max(d.getIntrinsicHeight(), 200);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return bmp;
    }

    /** Carrega Bitmap de uma URI local (galeria) */
    private Bitmap bitmapDeUri(Uri uri) throws Exception {
        android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
        opts.inSampleSize = 2; // reduz para não consumir muita memória
        try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
            return android.graphics.BitmapFactory.decodeStream(is, null, opts);
        }
    }

    /**
     * Desenha um bitmap centralizado e ajustado dentro de uma seção retangular do canvas.
     * Se o bitmap for null, deixa a seção com o fundo escuro.
     */
    private void desenharSecao(Canvas canvas, Bitmap bmp, int yOffset, int sectionH, int canvasW, int bgColor) {
        android.graphics.Paint bgPaint = new android.graphics.Paint();
        bgPaint.setColor(bgColor);
        canvas.drawRect(0, yOffset, canvasW, yOffset + sectionH, bgPaint);

        if (bmp == null) return;

        // Calcular escala para caber na seção mantendo proporção (fitCenter)
        float scaleX = (float) canvasW / bmp.getWidth();
        float scaleY = (float) sectionH / bmp.getHeight();
        float scale  = Math.min(scaleX, scaleY);

        int dstW = (int) (bmp.getWidth()  * scale);
        int dstH = (int) (bmp.getHeight() * scale);
        int left = (canvasW - dstW) / 2;
        int top  = yOffset + (sectionH - dstH) / 2;

        android.graphics.Rect dst = new android.graphics.Rect(left, top, left + dstW, top + dstH);
        canvas.drawBitmap(bmp, null, dst, null);
    }
}
