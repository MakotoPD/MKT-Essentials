package pl.makoto.essentials.data;

import java.util.List;

/**
 * Data model for a single inventory backup.
 * Serialized to/from JSON files in the backups directory.
 */
public class InventoryBackup {
    private long timestamp;
    private String reason;          // "death", "manual", "scheduled"
    private String note;            // optional note or death message
    private double deathX, deathY, deathZ; // only for death backups
    private String deathDimension;         // only for death backups
    private int experienceLevel;
    private int totalItems;
    private List<String> inventory; // NBT-serialized ItemStacks as JSON strings (36 slots)
    private List<String> armor;     // 4 items
    private String offhand;         // 1 item
    private List<CuriosBackupEntry> curios; // Curios items (slot type + index + NBT)

    public static class CuriosBackupEntry {
        private String slotType;
        private int slotIndex;
        private String itemNbt;

        public CuriosBackupEntry() {}
        public CuriosBackupEntry(String slotType, int slotIndex, String itemNbt) {
            this.slotType = slotType;
            this.slotIndex = slotIndex;
            this.itemNbt = itemNbt;
        }

        public String getSlotType() { return slotType; }
        public int getSlotIndex() { return slotIndex; }
        public String getItemNbt() { return itemNbt; }
    }

    public InventoryBackup() {}

    public InventoryBackup(long timestamp, String reason, String note,
                           int experienceLevel, int totalItems,
                           List<String> inventory, List<String> armor, String offhand) {
        this.timestamp = timestamp;
        this.reason = reason;
        this.note = note;
        this.experienceLevel = experienceLevel;
        this.totalItems = totalItems;
        this.inventory = inventory;
        this.armor = armor;
        this.offhand = offhand;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public double getDeathX() { return deathX; }
    public void setDeathX(double deathX) { this.deathX = deathX; }

    public double getDeathY() { return deathY; }
    public void setDeathY(double deathY) { this.deathY = deathY; }

    public double getDeathZ() { return deathZ; }
    public void setDeathZ(double deathZ) { this.deathZ = deathZ; }

    public String getDeathDimension() { return deathDimension; }
    public void setDeathDimension(String deathDimension) { this.deathDimension = deathDimension; }

    public int getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(int experienceLevel) { this.experienceLevel = experienceLevel; }

    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

    public List<String> getInventory() { return inventory; }
    public void setInventory(List<String> inventory) { this.inventory = inventory; }

    public List<String> getArmor() { return armor; }
    public void setArmor(List<String> armor) { this.armor = armor; }

    public String getOffhand() { return offhand; }
    public void setOffhand(String offhand) { this.offhand = offhand; }

    public List<CuriosBackupEntry> getCurios() { return curios; }
    public void setCurios(List<CuriosBackupEntry> curios) { this.curios = curios; }
}
