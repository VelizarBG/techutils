package dev.kikugie.techutils.event;

import fi.dy.masa.litematica.interfaces.ISchematicPlacementEventListener;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventHandler;
import org.jspecify.annotations.Nullable;

public class PlacementEventHandler implements ISchematicPlacementEventListener {
    @Override
    public void onPlacementAdded(SchematicPlacement placement) {

    }

    @Override
    public void onPlacementRemoved(SchematicPlacement placement) {

    }

    @Override
    public void onPlacementSelected(@Nullable SchematicPlacement prevPlacement, @Nullable SchematicPlacement selected) {
        if (selected != null) {
            SchematicPlacementEventHandler.getInstance().invokePostPlacementChange(this, selected);
        }
    }
}
