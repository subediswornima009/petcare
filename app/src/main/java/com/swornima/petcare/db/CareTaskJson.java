package com.swornima.petcare.db;

import com.swornima.petcare.CareTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a Pet's embedded care task checklist to/from a JSON string so it
 * can be stored in a single Room column (PetEntity.careTasksJson), mirroring
 * how it was previously stored as a nested array field in Firestore.
 */
public final class CareTaskJson {

    private CareTaskJson() {
    }

    public static String toJson(List<CareTask> tasks) {
        JSONArray array = new JSONArray();
        if (tasks != null) {
            for (CareTask task : tasks) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("id", task.getId());
                    obj.put("name", task.getName());
                    obj.put("category", task.getCategory());
                    obj.put("supplies", task.getSupplies());
                    obj.put("notes", task.getNotes());
                    obj.put("userId", task.getUserId());
                    obj.put("time", task.getTime());
                    obj.put("frequency", task.getFrequency());
                    obj.put("appointmentDate", task.getAppointmentDate());
                    obj.put("groomingDate", task.getGroomingDate());
                    obj.put("vaccinationDueDate", task.getVaccinationDueDate());
                    obj.put("medicationStartDate", task.getMedicationStartDate());
                    obj.put("medicationEndDate", task.getMedicationEndDate());
                    obj.put("completed", task.isCompleted());
                    obj.put("completedAtMillis", task.getCompletedAtMillis());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                array.put(obj);
            }
        }
        return array.toString();
    }

    public static List<CareTask> fromJson(String json) {
        List<CareTask> tasks = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return tasks;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                CareTask task = new CareTask(
                        obj.optString("id", null),
                        obj.optString("name", null),
                        obj.optString("category", null),
                        obj.optString("notes", null),
                        obj.optString("userId", null),
                        obj.optString("time", null),
                        obj.optString("frequency", null),
                        obj.optString("supplies", null));
                task.setAppointmentDate(obj.optString("appointmentDate", null));
                task.setGroomingDate(obj.optString("groomingDate", null));
                task.setVaccinationDueDate(obj.optString("vaccinationDueDate", null));
                task.setMedicationStartDate(obj.optString("medicationStartDate", null));
                task.setMedicationEndDate(obj.optString("medicationEndDate", null));
                task.setCompleted(obj.optBoolean("completed", false));
                task.setCompletedAtMillis(obj.optLong("completedAtMillis", 0));
                tasks.add(task);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return tasks;
    }
}
