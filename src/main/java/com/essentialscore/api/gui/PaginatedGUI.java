package com.essentialscore.api.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI implementation that supports pagination for displaying large collections of items.
 */
public class PaginatedGUI extends StandardGUI {
    
    private static final int NAVIGATION_ROW = 5; // Bottom row (0-indexed)
    private static final int PREV_PAGE_SLOT = 45; // First slot in the bottom row
    private static final int NEXT_PAGE_SLOT = 53; // Last slot in the bottom row
    private static final int INFO_SLOT = 49; // Middle slot in the bottom row
    
    private final List<ItemStack> items;
    private final List<GUIClickHandler> handlers;
    private final int itemsPerPage;
    private final int contentRows;
    private int currentPage;
    private final int totalPages;
    
    /**
     * Creates a new paginated GUI.
     *
     * @param moduleId The module ID
     * @param title The GUI title
     * @param rows The number of rows
     * @param theme The GUI theme
     * @param items The items to display
     * @param handlers The click handlers for items
     */
    public PaginatedGUI(String moduleId, String title, int rows, GUITheme theme, 
                          List<ItemStack> items, List<GUIClickHandler> handlers) {
        super(moduleId, title, rows, theme);
        
        this.items = new ArrayList<>(items);
        this.handlers = handlers != null ? new ArrayList<>(handlers) : new ArrayList<>();
        this.currentPage = 0;
        
        // Content rows exclude the navigation row
        this.contentRows = rows - 1;
        this.itemsPerPage = contentRows * 9;
        
        // Calculate total pages
        this.totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
        
        // Initialize navigation
        initializeNavigation();
        
        // Show the first page
        showPage(0);
    }
    
    /**
     * Creates a new paginated GUI.
     *
     * @param moduleId The module ID
     * @param title The GUI title
     * @param rows The number of rows
     * @param theme The GUI theme
     * @param items The items to display
     */
    public PaginatedGUI(String moduleId, String title, int rows, GUITheme theme, List<ItemStack> items) {
        this(moduleId, title, rows, theme, items, null);
    }
    
    /**
     * Gets the current page.
     *
     * @return The current page
     */
    public int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * Gets the total number of pages.
     *
     * @return The total pages
     */
    public int getTotalPages() {
        return totalPages;
    }
    
    /**
     * Adds an item to the paginated GUI.
     *
     * @param item The item to add
     * @param handler The click handler
     */
    public void addItem(ItemStack item, GUIClickHandler handler) {
        items.add(item);
        
        // Add a handler if provided, or null otherwise
        if (handler != null) {
            while (handlers.size() < items.size() - 1) {
                handlers.add(null);
            }
            handlers.add(handler);
        }
        
        // Recalculate pages and update display
        updateDisplay();
    }
    
    /**
     * Adds an item to the paginated GUI.
     *
     * @param item The item to add
     */
    public void addItem(ItemStack item) {
        addItem(item, null);
    }
    
    /**
     * Removes an item from the paginated GUI.
     *
     * @param index The item index
     */
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            
            if (index < handlers.size()) {
                handlers.remove(index);
            }
            
            // Recalculate pages and update display
            updateDisplay();
        }
    }
    
    /**
     * Sets the current page.
     *
     * @param page The page to show
     */
    public void setPage(int page) {
        if (page >= 0 && page < totalPages) {
            currentPage = page;
            showPage(currentPage);
        }
    }
    
    /**
     * Goes to the next page.
     */
    public void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            showPage(currentPage);
        }
    }
    
    /**
     * Goes to the previous page.
     */
    public void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            showPage(currentPage);
        }
    }
    
    @Override
    public void onClick(Player player, int slot, InventoryClickEvent event) {
        // Handle navigation clicks
        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            prevPage();
            return;
        } else if (slot == NEXT_PAGE_SLOT && currentPage < totalPages - 1) {
            nextPage();
            return;
        } else if (slot == INFO_SLOT) {
            // Info button does nothing
            return;
        }
        
        // Check if the click is in the content area
        if (slot < itemsPerPage) {
            int itemIndex = currentPage * itemsPerPage + slot;
            
            if (itemIndex < items.size()) {
                // Handle the click if there's a handler for this item
                if (itemIndex < handlers.size() && handlers.get(itemIndex) != null) {
                    handlers.get(itemIndex).onClick(player, slot, event);
                }
                return;
            }
        }
        
        // For slots not handled above, delegate to the parent
        super.onClick(player, slot, event);
    }
    
    /**
     * Shows the specified page.
     *
     * @param page The page to show
     */
    private void showPage(int page) {
        // Clear the content area
        for (int slot = 0; slot < itemsPerPage; slot++) {
            setItem(slot, null);
        }
        
        // Calculate the start and end indices for this page
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        
        // Add the items for this page
        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            setItem(slot, items.get(i));
        }
        
        // Update the page info
        updatePageInfo();
    }
    
    /**
     * Initializes the navigation buttons.
     */
    private void initializeNavigation() {
        // Calculate the start slot for the navigation row
        int navigationRowStart = NAVIGATION_ROW * 9;
        
        // Add background for the navigation row
        ItemStack background = getTheme().getBackgroundItem();
        for (int slot = navigationRowStart; slot < navigationRowStart + 9; slot++) {
            setItem(slot, background);
        }
        
        // Add navigation buttons
        updateNavigationButtons();
    }
    
    /**
     * Updates the navigation buttons based on the current page.
     */
    private void updateNavigationButtons() {
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(getTheme().formatText("Previous Page"));
                prevButton.setItemMeta(meta);
            }
            setItem(PREV_PAGE_SLOT, prevButton);
        } else {
            // Disabled button
            ItemStack disabledButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = disabledButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(getTheme().getSecondaryColor() + "Previous Page");
                disabledButton.setItemMeta(meta);
            }
            setItem(PREV_PAGE_SLOT, disabledButton);
        }
        
        // Next page button
        if (currentPage < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta meta = nextButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(getTheme().formatText("Next Page"));
                nextButton.setItemMeta(meta);
            }
            setItem(NEXT_PAGE_SLOT, nextButton);
        } else {
            // Disabled button
            ItemStack disabledButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = disabledButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(getTheme().getSecondaryColor() + "Next Page");
                disabledButton.setItemMeta(meta);
            }
            setItem(NEXT_PAGE_SLOT, disabledButton);
        }
        
        // Update page info
        updatePageInfo();
    }
    
    /**
     * Updates the page info display.
     */
    private void updatePageInfo() {
        ItemStack infoButton = new ItemStack(Material.PAPER);
        ItemMeta meta = infoButton.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(getTheme().formatText("Page " + (currentPage + 1) + " of " + totalPages));
            
            List<String> lore = new ArrayList<>();
            lore.add(getTheme().getSecondaryColor() + "Items: " + items.size());
            meta.setLore(lore);
            
            infoButton.setItemMeta(meta);
        }
        
        setItem(INFO_SLOT, infoButton);
    }
    
    /**
     * Updates the display after changes to the items.
     */
    private void updateDisplay() {
        // Recalculate total pages
        int newTotalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
        
        // Adjust current page if needed
        if (currentPage >= newTotalPages) {
            currentPage = Math.max(0, newTotalPages - 1);
        }
        
        // Update the display
        showPage(currentPage);
        updateNavigationButtons();
    }
} 
