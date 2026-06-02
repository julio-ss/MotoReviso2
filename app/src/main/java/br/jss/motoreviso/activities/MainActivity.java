package br.jss.motoreviso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import br.jss.motoreviso.R;
import br.jss.motoreviso.fragments.ConfiguracoesFragment;
import br.jss.motoreviso.fragments.DashboardFragment;
import br.jss.motoreviso.fragments.ManutencoesFragment;
import br.jss.motoreviso.fragments.TrajetosFragment;
import br.jss.motoreviso.fragments.VeiculosFragment;
import br.jss.motoreviso.utils.SystemBarHelper;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private FrameLayout frameLayout;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SystemBarHelper.applySystemBarPadding(this, findViewById(R.id.frame_layout));

        bottomNavigation = findViewById(R.id.bottom_navigation);
        frameLayout = findViewById(R.id.frame_layout);
        fragmentManager = getSupportFragmentManager();

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                carregarFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_veiculos) {
                carregarFragment(new VeiculosFragment());
                return true;
            } else if (itemId == R.id.nav_manutencoes) {
                carregarFragment(new ManutencoesFragment());
                return true;
            } else if (itemId == R.id.nav_trajetos) {
                carregarFragment(new TrajetosFragment());
                return true;
            } else if (itemId == R.id.nav_config) {
                carregarFragment(new ConfiguracoesFragment());
                return true;
            }

            return false;
        });

        if (savedInstanceState == null) {
            // Set Dashboard as the initial screen
            bottomNavigation.setSelectedItemId(R.id.nav_inicio);
            carregarFragment(new DashboardFragment());
        }
    }

    private void carregarFragment(Fragment fragment) {
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