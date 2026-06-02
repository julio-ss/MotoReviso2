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
import br.jss.motoreviso.fragments.TrajetosFragment;
import br.jss.motoreviso.fragments.VeiculosFragment;
import br.jss.motoreviso.utils.SystemBarHelper;

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

        bottomNavigation = findViewById(R.id.bottom_navigation);
        fabRastreamento = findViewById(R.id.fab_rastreamento);
        frameLayout = findViewById(R.id.frame_layout);
        fragmentManager = getSupportFragmentManager();

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                carregarFragment(new DashboardFragment());
                fabRastreamento.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_veiculos) {
                carregarFragment(new VeiculosFragment());
                fabRastreamento.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_manutencoes) {
                carregarFragment(new ManutencoesFragment());
                fabRastreamento.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_config) {
                carregarFragment(new ConfiguracoesFragment());
                fabRastreamento.setVisibility(android.view.View.VISIBLE);
                return true;
            } else if (itemId == R.id.nav_placeholder) {
                // Item vazio no centro (substituído pelo FAB) - não fazer nada
                return false;
            }

            return false;
        });

        // Listener do FAB para navegação ao Rastreamento
        fabRastreamento.setOnClickListener(v -> {
            bottomNavigation.setSelectedItemId(R.id.nav_trajetos);
            carregarFragment(new TrajetosFragment());
        });

        if (savedInstanceState == null) {
            // Set Dashboard as the initial screen
            bottomNavigation.setSelectedItemId(R.id.nav_inicio);
            carregarFragment(new DashboardFragment());
        }
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
}