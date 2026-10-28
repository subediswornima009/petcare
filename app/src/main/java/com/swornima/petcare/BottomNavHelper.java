package com.swornima.petcare;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Wires up the shared bottom navigation bar (Home / My Pets / Care Routine /
 * Places / Expenses) that's included in each of those five screens. Each
 * screen is a separate Activity, so switching tabs launches the target
 * Activity with FLAG_ACTIVITY_CLEAR_TOP + singleTop so it behaves like a tab
 * switch (no stacking duplicate screens, no back-stack buildup) rather than
 * normal forward navigation.
 */
public final class BottomNavHelper {

    public enum Tab { HOME, MY_PETS, CARE_ROUTINE, PLACES, EXPENSES }

    private BottomNavHelper() {
    }

    public static void setup(Activity activity, Tab current) {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        int selectedId;
        switch (current) {
            case MY_PETS:
                selectedId = R.id.navMyPets;
                break;
            case CARE_ROUTINE:
                selectedId = R.id.navCareRoutine;
                break;
            case PLACES:
                selectedId = R.id.navPlaces;
                break;
            case EXPENSES:
                selectedId = R.id.navExpenses;
                break;
            case HOME:
            default:
                selectedId = R.id.navHome;
                break;
        }
        bottomNav.setSelectedItemId(selectedId);

        bottomNav.setOnItemSelectedListener(item -> {
            Class<?> target = null;
            if (item.getItemId() == R.id.navHome) {
                target = current == Tab.HOME ? null : MainActivity.class;
            } else if (item.getItemId() == R.id.navMyPets) {
                target = current == Tab.MY_PETS ? null : PetListActivity.class;
            } else if (item.getItemId() == R.id.navCareRoutine) {
                target = current == Tab.CARE_ROUTINE ? null : MyItemsActivity.class;
            } else if (item.getItemId() == R.id.navPlaces) {
                target = current == Tab.PLACES ? null : PlacesActivity.class;
            } else if (item.getItemId() == R.id.navExpenses) {
                target = current == Tab.EXPENSES ? null : ExpenseActivity.class;
            }

            if (target != null) {
                Intent intent = new Intent(activity, target);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
            }
            return true;
        });
    }
}
