package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.modloaders.InstalledModAdapter;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;

public class ManageModsFragment extends Fragment {

    public static final String TAG = "ManageModsFragment";

    public ManageModsFragment() {
        super(R.layout.fragment_manage_mods);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ImageButton backButton = view.findViewById(R.id.manage_mods_back);
        ImageButton addButton = view.findViewById(R.id.manage_mods_add);
        TextView title = view.findViewById(R.id.manage_mods_title);
        RecyclerView recycler = view.findViewById(R.id.manage_mods_recycler);
        View emptyState = view.findViewById(R.id.manage_mods_empty);

        backButton.setOnClickListener(v -> Tools.removeCurrentFragment(requireActivity()));

        addButton.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), ModsSearchFragment.class, ModsSearchFragment.TAG, null));

        String profileName = getCurrentProfileName();
        title.setText(profileName.isEmpty()
                ? getString(R.string.mcl_button_manage_mods)
                : profileName + " - Mods");

        File modsDir = getModsDir();
        InstalledModAdapter adapter = new InstalledModAdapter(modsDir, isEmpty -> {
            recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
    }

    private String getCurrentProfileName() {
        try {
            Instance instance = Instances.loadSelectedInstance();
            if (instance != null) {
                return instance.getName();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private File getModsDir() {
        try {
            Instance instance = Instances.loadSelectedInstance();
            if (instance != null) {
                File gameDir = instance.getGameDirectory();
                return new File(gameDir, "mods");
            }
        } catch (Exception ignored) {
        }
        return new File(Tools.DIR_GAME_NEW, "mods");
    }
}
