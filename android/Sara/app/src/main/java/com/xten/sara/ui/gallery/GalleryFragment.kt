package com.xten.sara.ui.gallery

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.xten.sara.SaraApplication.Companion.dropdownSoftKeyboard
import com.xten.sara.SaraApplication.Companion.showToast
import com.xten.sara.data.Gallery
import com.xten.sara.databinding.FragmentGalleryBinding
import com.xten.sara.util.constants.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GalleryFragment : Fragment() {

    private lateinit var binding: FragmentGalleryBinding
    private val galleryViewModel: GalleryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGalleryBinding.inflate(layoutInflater)
        return setBinding().root
    }

    private fun setBinding() = binding.apply {
        viewModel = galleryViewModel
        galleryViewModel.updateGallery()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        subscribeToObservers()
    }


    private val albumTypeItemAdapter by lazy {
        GalleryItemAdapter(TYPE_ALBUM).apply {
            setOnItemClickListener {
                navigateToGalleryDetails(it)
            }
        }
    }
    private val listTypeItemAdapter by lazy {
        GalleryItemAdapter(TYPE_LIST).apply {
            setOnItemClickListener {
                Log.e(TAG, "$it: ", )
                navigateToGalleryDetails(it)
            }
        }
    }
    private fun initView() = binding.apply {
        setRecyclerViewItemType(TYPE_ALBUM)

        btnAlbum.setOnCheckedChangeListener { _, isChecked ->
            setTypeButtonAction(isChecked)
        }

        editSearch.setOnKeyListener { _, keyCode, event ->
            when(keyCode) {
                KeyEvent.KEYCODE_ENTER -> handleEnterKeyEvent()
                else -> return@setOnKeyListener false
            }
        }

        btnReset.setOnClickListener {
            setResetButtonAction()
        }

    }

    @Inject
    lateinit var inputManager: InputMethodManager
    private fun handleEnterKeyEvent(): Boolean = binding.editSearch.run {
        dropdownSoftKeyboard(requireActivity(), inputManager)
        galleryViewModel.apply {
            val input = getCurInput()
            if(input.isNotBlank()) {
                requestSearch()
                binding.recyclerView.scrollToPosition(DEFAULT_POSITION)
            }
            else showToast(requireContext(), MESSAGE_WARNING_EDIT)
        }

        true
    }

    private fun setRecyclerViewItemType(type: Int) = binding.recyclerView.apply {
        when(type) {
            TYPE_ALBUM -> {
                val gridLayoutManager = GridLayoutManager(requireContext(), GRID_COL_TYPE_1)
                layoutManager = gridLayoutManager
                adapter = albumTypeItemAdapter
            }
            else -> {
                val linearLayoutManager = LinearLayoutManager(requireContext())
                layoutManager = linearLayoutManager
                adapter = listTypeItemAdapter
            }
        }
        scrollToPosition(DEFAULT_POSITION)
    }

    private fun setTypeButtonAction(isChecked: Boolean) = when {
        isChecked -> setRecyclerViewItemType(TYPE_ALBUM)
        else -> setRecyclerViewItemType(TYPE_LIST)
    }

    private fun setResetButtonAction() = binding.apply {
        editSearch.text?.clear()
        galleryViewModel.resetGallery()
        dropdownSoftKeyboard(requireActivity(), inputManager)
        recyclerView.smoothScrollToPosition(DEFAULT_POSITION)
    }

    private fun subscribeToObservers() = binding.apply {
        with(galleryViewModel) {
            galleryList.observe(viewLifecycleOwner) {
                it?.let {
                    val list = it.sortedByDescending { image ->
                        image.createdAt
                    }
                    albumTypeItemAdapter.submitData(list)
                    listTypeItemAdapter.submitData(list)

                    binding.recyclerView.smoothScrollToPosition(DEFAULT_POSITION)
                    if(getCurInput().isBlank()) return@let
                    if(list.isEmpty()) showToast(requireContext(), MESSAGE_RESULT_SEARCH_FAIL)
                }
            }
        }
    }

    private fun navigateToGalleryDetails(gallery: Gallery) {
        val action = GalleryFragmentDirections.actionGalleryFragmentToGalleryDetailsFragment(
            gallery
        )
        findNavController().navigate(action)
    }


}