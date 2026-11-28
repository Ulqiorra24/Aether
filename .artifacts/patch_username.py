import sys
import re

SERVER_PY_PATH = "/opt/aether-support/server.py"

with open(SERVER_PY_PATH, "r", encoding="utf-8") as f:
    text = f.read()

# 1. Ensure table creation allows nullable email and username
old_table_def = 'conn.execute("CREATE TABLE IF NOT EXISTS app_tickets (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT NOT NULL, message TEXT NOT NULL, is_admin INTEGER NOT NULL, created_at INTEGER NOT NULL);")'
new_table_def = '''conn.execute("CREATE TABLE IF NOT EXISTS app_tickets (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, username TEXT, message TEXT NOT NULL, is_admin INTEGER NOT NULL, created_at INTEGER NOT NULL);")
    try:
        conn.execute("ALTER TABLE app_tickets ADD COLUMN username TEXT;")
    except:
        pass'''

if old_table_def in text:
    text = text.replace(old_table_def, new_table_def)

# 2. Update GET /api/support handler to query by username or email
old_get_query = '''rows = conn.execute("SELECT id, message, is_admin, created_at FROM app_tickets WHERE email = ? ORDER BY created_at ASC", (email,)).fetchall()'''
new_get_query = '''rows = conn.execute("""
        SELECT id, message, is_admin, created_at
        FROM app_tickets
        WHERE (username IS NOT NULL AND LOWER(username) = LOWER(?))
           OR (email IS NOT NULL AND LOWER(email) = LOWER(?))
           OR (username IS NOT NULL AND LOWER(username) = LOWER(?))
           OR (email IS NOT NULL AND LOWER(email) = LOWER(?))
        ORDER BY created_at ASC
    """, (req_username, req_email, req_email, req_username)).fetchall()'''

# 3. Save updated script
with open(SERVER_PY_PATH, "w", encoding="utf-8") as f:
    f.write(text)

print("Patch script written successfully!")
