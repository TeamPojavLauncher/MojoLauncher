package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.downloader.DownloadUtils;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.modloaders.modpacks.api.CurseforgeApi;
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.DownloadManager;

import java.io.File;
import java.io.IOException;

public class ModsSearchFragment extends Fragment {

    public static final String TAG = "ModsSearchFragment";
    private static final String CURSEFORGE_API_KEY = "YOUR_API_KEY_HERE";

    private CurseforgeApi mCurseforgeApi;
    private ModItemAdapter mAdapter;
    private SearchResult mCurrentSearchResult;
    private SearchFilters mSearchFilters;

    public ModsSearchFragment() {
        super(R.layout.fragment_mod_search);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ImageButton backButton = view.findViewById(R.id.mods_search_back);
        Button searchButton = view.findViewById(R.id.mods_search_button);
        Button filterButton = view.findViewById(R.id.mods_filter_button);
        Button loadMoreButton = view.findViewById(R.id.mods_load_more_button);
        EditText searchEditText = view.findViewById(R.id.mods_search_edit);
        RecyclerView recyclerView = view.findViewById(R.id.mods_recycler);
        ProgressBar progressBar = view.findViewById(R.id.mods_progress);

        mCurseforgeApi = new CurseforgeApi(CURSEFORGE_API_KEY);
        mSearchFilters = new SearchFilters();
        mSearchFilters.isModpack = false;

        backButton.setOnClickListener(v -> Tools.removeCurrentFragment(requireActivity()));

        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                mSearchFilters.name = query;
                mCurrentSearchResult = null;
                performSearch(progressBar);
            }
        });

        filterButton.setOnClickListener(v -> displayFilterDialog());

        searchEditText.setHint(R.string.hint_search_mod);

        mAdapter = new ModItemAdapter((modDetail, selectedVersion) ->
                handleInstallation(requireContext(), modDetail, selectedVersion, progressBar));

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(mAdapter);

        loadMoreButton.setOnClickListener(v -> {
            if (mCurrentSearchResult != null) {
                performSearch(progressBar);
            }
        });

        searchMods(null);
    }

    private void performSearch(ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                SearchResult result = mCurseforgeApi.searchMod(mSearchFilters, mCurrentSearchResult);
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (result != null) {
                        mCurrentSearchResult = result;
                        if (mCurrentSearchResult == null) {
                            mAdapter.setMods(result.results);
                        } else {
                            mAdapter.addMods(result.results);
                        }
                    } else {
                        Toast.makeText(requireContext(), R.string.search_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Search error", e);
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), R.string.search_failed, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void searchMods(String query) {
        if (query != null && !query.isEmpty()) {
            mSearchFilters.name = query;
        }
        performSearch(null);
    }

    private void displayFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.filter_mods)
                .setMessage("Filter options coming soon")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void handleInstallation(Context context, ModDetail modDetail, int selectedVersion, ProgressBar progressBar) {
        if (selectedVersion < 0 || selectedVersion >= modDetail.versionUrls.length) {
            Toast.makeText(context, R.string.invalid_version, Toast.LENGTH_SHORT).show();
            return;
        }

        String url = modDetail.versionUrls[selectedVersion];
        if (url == null || url.isEmpty()) {
            Toast.makeText(context, R.string.modpack_install_download_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        String finalFileName = url.substring(url.lastIndexOf('/') + 1);
        File modsDir = getModsDir();
        modsDir.mkdirs();
        File destFile = new File(modsDir, finalFileName);

        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                DownloadUtils.downloadFile(url, destFile);
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
                Tools.runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Context themedCtx = new ContextThemeWrapper(context, R.style.AppTheme);
                    new AlertDialog.Builder(themedCtx)
                            .setTitle(modDetail.title)
                            .setMessage(context.getString(R.string.mod_install_success, finalFileName))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            } catch (Exception e) {
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
                Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e);
                progressBar.setVisibility(View.GONE);
            }
        }).start();
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
