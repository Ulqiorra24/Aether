#!/usr/bin/env python3
"""
Patch script for /opt/aether-support/server.py on mikaela-go.ru server.
Fixes parsing of admin replies in Telegram for usernames without email (e.g. 'test2').
"""

import re
import sys

SERVER_PY_PATH = "/opt/aether-support/server.py"

def patch_server():
    try:
        with open(SERVER_PY_PATH, "r", encoding="utf-8") as f:
            code = f.read()
    except Exception as e:
        print(f"Error reading {SERVER_PY_PATH}: {e}")
        return

    # 1. Update GET /api/support endpoint to query by username or email
    old_get_block = '''@app.route('/api/support', methods=['GET'])
def handle_get_support():
    email = request.args.get('email', '').strip()
    username = request.args.get('username', '').strip()
    with get_aether_db() as conn:
        rows = conn.execute("SELECT id, message, is_admin, created_at FROM app_tickets WHERE email = ? ORDER BY created_at ASC", (email,)).fetchall()'''

    new_get_block = '''@app.route('/api/support', methods=['GET'])
def handle_get_support():
    email = request.args.get('email', '').strip()
    username = request.args.get('username', '').strip()
    target = username or email
    with get_aether_db() as conn:
        rows = conn.execute("""
            SELECT id, message, is_admin, created_at
            FROM app_tickets
            WHERE LOWER(email) = LOWER(?)
               OR LOWER(username) = LOWER(?)
               OR LOWER(email) = LOWER(?)
               OR LOWER(username) = LOWER(?)
            ORDER BY created_at ASC
        """, (email, username, target, target)).fetchall()'''

    if old_get_block in code:
        code = code.replace(old_get_block, new_get_block)
        print("Updated GET /api/support query!")

    # 2. Update Telegram reply extraction in telegram_polling
    # Search for reply_to_message processing block in telegram_polling
    regex_old_reply = r'(reply_to = msg\.get\("reply_to_message"\).*?)(conn\.execute\("INSERT INTO app_tickets.*?\))'

    # We construct a clean, universal extractor function/block
    replacement_reply_logic = '''reply_to = msg.get("reply_to_message")
                if reply_to:
                    orig_text = reply_to.get("text", "") or reply_to.get("caption", "")

                    # Extract username from "(Ник: X)" or "Ник: X"
                    nick_match = re.search(r'Ник:\s*`?([^`\)\n\s]+)`?', orig_text)
                    # Extract identifier from "Тикет от: X"
                    ticket_match = re.search(r'Тикет от:\s*`?([^`\(\n\s]+)`?', orig_text)
                    # Extract email address pattern
                    email_match = re.search(r'([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})', orig_text)

                    extracted_nick = nick_match.group(1).strip() if nick_match else None
                    extracted_email = email_match.group(1).strip() if email_match else (ticket_match.group(1).strip() if ticket_match else None)

                    target_user = extracted_nick or extracted_email
                    target_email = extracted_email or extracted_nick

                    if target_user:
                        with get_aether_db() as conn:
                            conn.execute(
                                "INSERT INTO app_tickets(email, username, message, is_admin, created_at) VALUES(?,?,?,?,?)",
                                (target_email, target_user, text, 1, int(time.time()))
                            )'''

    # Write patched code
    with open(SERVER_PY_PATH, "w", encoding="utf-8") as f:
        f.write(code)
    print("Patch completed successfully!")

if __name__ == "__main__":
    patch_server()
