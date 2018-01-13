package com.bijoysingh.quicknote.database.external

import android.content.Context
import com.bijoysingh.quicknote.MaterialNotes
import com.bijoysingh.quicknote.database.Note
import com.bijoysingh.quicknote.utils.genImportedNote
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase


/**
 * Functions for Database Reference for Firebase Notes
 */

fun noteDatabaseReference(context: Context, userId: String) {
  if (MaterialNotes.firebaseNote !== null) {
    return
  }
  MaterialNotes.firebaseNote = FirebaseDatabase
      .getInstance()
      .getReference()
      .child("notes")
      .child(userId)
  setListener(context)
}

fun removeNoteDatabaseReference() {
  MaterialNotes.firebaseNote = null
}

fun insertNoteToFirebase(note: FirebaseNote) {
  if (MaterialNotes.firebaseNote == null) {
    return
  }
  MaterialNotes.firebaseNote!!.child(note.uuid).setValue(note)
}

fun deleteFromFirebase(note: FirebaseNote) {
  if (MaterialNotes.firebaseNote == null) {
    return
  }
  MaterialNotes.firebaseNote!!.child(note.uuid).removeValue()
}

private fun setListener(context: Context) {
  if (MaterialNotes.firebaseNote === null) {
    return
  }
  MaterialNotes.firebaseNote!!.addChildEventListener(object : ChildEventListener {
    override fun onCancelled(p0: DatabaseError?) {
      // Ignore cancelled
    }

    override fun onChildMoved(snapshot: DataSnapshot?, p1: String?) {
      // Ignore moved child
    }

    override fun onChildChanged(snapshot: DataSnapshot?, p1: String?) {
      onChildAdded(snapshot, p1)
    }

    override fun onChildAdded(snapshot: DataSnapshot?, p1: String?) {
      handleNoteChange(snapshot, fun(note, existingNote, isSame) {
        if (existingNote === null) {
          note.saveWithoutSync(context)
          return
        }
        if (!isSame) {
          existingNote.copyNote(note).saveWithoutSync(context)
        }
      })
    }

    override fun onChildRemoved(snapshot: DataSnapshot?) {
      handleNoteChange(snapshot, fun(_, existingNote, _) {
        if (existingNote !== null) {
          existingNote.deleteWithoutSync(context)
        }
      })
    }

    fun handleNoteChange(
        snapshot: DataSnapshot?,
        listener: (Note, Note?, Boolean) -> Unit) {
      if (snapshot === null) {
        return
      }
      try {
        val note = snapshot.getValue(FirebaseNote::class.java)
        if (note === null) {
          return
        }

        val notifiedNote = genImportedNote(note)
        val existingNote = Note.db(context).getByUUID(note.uuid)
        var isSame = false
        if (existingNote !== null) {
          isSame = notifiedNote.isEqual(existingNote)
        }

        listener(notifiedNote, existingNote, isSame)
      } catch (e: Exception) {
        // Ignore if exception
      }
    }
  })
}