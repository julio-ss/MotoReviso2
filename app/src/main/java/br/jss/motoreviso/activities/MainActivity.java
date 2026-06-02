package br.jss.motoreviso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import br.jss.motoreviso.R;
import br.jss.motoreviso.fragments.ConfiguracoesFragment;
import br.jss.motoreviso.fragments.DashboardFragment;
import br.jss.motoreviso.fragments.ManutencoesFragment;
import br.jss.motoreviso.fragments.RastreamentoEmTempoRealFragment;
import br.jss.motoreviso.fragments.VeiculosFragment;
import br.jss.motoreviso.utils.SystemBarHelper;
import br.jss.motoreviso.activities.GaleriaVeiculoActivity;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabRastreamento;
    private FrameLayout frameLayout;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SystemBarHelper.applySystemBarPadding(this, findViewById(R.id.frame_layout));

        fabRastreamento = findViewById(R.id.fab_rastreamento);
        frameLayout = findViewById(R.id.frame_layout);
        fragmentManager = getSupportFragmentManager();

        // Setup custom navigation items
        setupCustomNavigation();

        // Listener do FAB para navegação ao Rastreamento em Tempo Real
        fabRastreamento.setOnClickListener(v -> {
            carregarFragment(new RastreamentoEmTempoRealFragment());
        });

        if (savedInstanceState == null) {
            // Set Dashboard as the initial screen
            carregarFragment(new DashboardFragment());
        }
    }

    private void setupCustomNavigation() {
        findViewById(R.id.nav_item_inicio).setOnClickListener(v -> carregarFragment(new DashboardFragment()));
        findViewById(R.id.nav_item_garagem).setOnClickListener(v -> carregarFragment(new VeiculosFragment()));
        findViewById(R.id.nav_item_manutencao).setOnClickListener(v -> carregarFragment(new ManutencoesFragment()));
        findViewById(R.id.nav_item_mais).setOnClickListener(v -> abrirMenuCascata());
    }

    public void carregarFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                R.anim.anim_slide_in,
                R.anim.anim_slide_out,
                R.anim.anim_slide_in,
                R.anim.anim_slide_out
        );
        transaction.replace(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void abrirDetalheVeiculo(String veiculoId) {
        Intent intent = new Intent(this, DetalheVeiculoActivity.class);
        intent.putExtra("VEICULO_ID", veiculoId);
        startActivity(intent);
        overridePendingTransition(R.anim.anim_fade_in, R.anim.anim_fade_out);
    }

    private void abrirMenuCascata() {
        // Encontrar a view do ícone "Mais" na navbar
        android.view.View navMaisView = bottomNavigation.findViewById(R.id.nav_mais);
        if (navMaisView == null) {
            // Se não encontrar, mostrar PopupMenu no ponto central inferior
            navMaisView = bottomNavigation;
        }

        // Criar um PopupMenu com as opções adicionais
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, navMaisView);
        popupMenu.inflate(R.menu.menu_mais);

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_config) {
                carregarFragment(new ConfiguracoesFragment());
                return true;
            } else if (id == R.id.nav_galeria) {
                Intent intent = new Intent(MainActivity.this, GaleriaVeiculoActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }
}