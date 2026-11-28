import sys

with open("/opt/aether-support/server.py", "r", encoding="utf-8") as f:
    code = f.read()

# 1. Update inline_kb in "🎁 Акции (Триал)"
old_kb = '''                            inline_kb = {
                                "inline_keyboard": [
                                    [{"text": f"Вкл/Выкл (Сейчас: {is_enabled})", "callback_data": "trial_toggle"}],
                                    [{"text": f"Изменить на 1 день", "callback_data": "trial_setdays_1"}, {"text": f"На 3 дня", "callback_data": "trial_setdays_3"}],
                                    [{"text": f"На 7 дней", "callback_data": "trial_setdays_7"}, {"text": f"На 14 дней", "callback_data": "trial_setdays_14"}],
                                    [{"text": f"На 31 день", "callback_data": "trial_setdays_31"}],
                                    [{"text": "Снять запрет с 1 юзера", "callback_data": "trial_clearone"}],
                                    [{"text": "Снять запрет СО ВСЕХ (Опасно)", "callback_data": "trial_clearall"}]
                                ]
                            }'''

new_kb = '''                            inline_kb = {
                                "inline_keyboard": [
                                    [{"text": f"Вкл/Выкл (Сейчас: {is_enabled})", "callback_data": "trial_toggle"}],
                                    [{"text": f"Изменить на 1 день", "callback_data": "trial_setdays_1"}, {"text": f"На 3 дня", "callback_data": "trial_setdays_3"}],
                                    [{"text": f"На 7 дней", "callback_data": "trial_setdays_7"}, {"text": f"На 14 дней", "callback_data": "trial_setdays_14"}],
                                    [{"text": f"На 31 день", "callback_data": "trial_setdays_31"}],
                                    [{"text": "Снять запрет", "callback_data": "trial_clearone"}, {"text": "Запретить юзеру", "callback_data": "trial_blockone"}],
                                    [{"text": "📋 Кто воспользовался", "callback_data": "trial_list"}],
                                    [{"text": "Снять запрет СО ВСЕХ (Опасно)", "callback_data": "trial_clearall"}]
                                ]
                            }'''

if old_kb in code:
    code = code.replace(old_kb, new_kb)

# 2. Add callback handlers for blockone and list
old_cb = '''                                elif action == "clearone":
                                    try:
                                        async with get_session() as s2:
                                            await s2.post(f"https://api.telegram.org/bot{TG_TOKEN}/answerCallbackQuery", json={"callback_query_id": cq_id})
                                        await tg_send_message(chat_id, "Отправь email пользователя, чтобы снять с него ограничение на триал.\\n(Напиши: `Сброс юзер@mail.com`)")
                                    except: pass'''

new_cb = '''                                elif action == "clearone":
                                    try:
                                        async with get_session() as s2:
                                            await s2.post(f"https://api.telegram.org/bot{TG_TOKEN}/answerCallbackQuery", json={"callback_query_id": cq_id})
                                        await tg_send_message(chat_id, "Отправь email или ник пользователя, чтобы снять с него ограничение на триал.\\n(Напиши: `Сброс юзер@mail.com`)")
                                    except: pass
                                elif action == "blockone":
                                    try:
                                        async with get_session() as s2:
                                            await s2.post(f"https://api.telegram.org/bot{TG_TOKEN}/answerCallbackQuery", json={"callback_query_id": cq_id})
                                        await tg_send_message(chat_id, "Отправь email или ник пользователя, чтобы заблокировать ему триал.\\n(Напиши: `Блокировка юзер@mail.com`)")
                                    except: pass
                                elif action == "list":
                                    with get_aether_db() as conn:
                                        rows = conn.execute("SELECT user_identifier, claimed_at FROM trial_claims ORDER BY claimed_at DESC LIMIT 20").fetchall()
                                    if not rows:
                                        text_msg = "🤷 Никто еще не воспользовался акцией."
                                    else:
                                        text_msg = "🎁 **Список пользователей, воспользовавшихся акцией:**\\n\\n"
                                        for u_id, c_time in rows:
                                            import datetime
                                            dt = datetime.datetime.fromtimestamp(c_time).strftime('%d.%m.%Y %H:%M')
                                            text_msg += f"▪️ `{u_id}` — {dt}\\n"
                                    try:
                                        async with get_session() as s2:
                                            await s2.post(f"https://api.telegram.org/bot{TG_TOKEN}/answerCallbackQuery", json={"callback_query_id": cq_id, "text": "Список загружен"})
                                        await tg_send_message(chat_id, text_msg)
                                    except: pass'''

if old_cb in code:
    code = code.replace(old_cb, new_cb)

# 3. Add text handler for "Блокировка "
old_txt = '''                        elif text.startswith("Сброс "):
                            email_to_reset = text[6:].strip()
                            with get_aether_db() as conn:
                                cur = conn.cursor()
                                cur.execute("DELETE FROM trial_claims WHERE LOWER(user_identifier) = LOWER(?)", (email_to_reset,))
                                deleted = cur.rowcount
                                conn.commit()
                            await tg_send_message(chat_id, f"✅ Ограничение на триал снято с `{email_to_reset}` (удалено {deleted} записей).")
                            continue'''

new_txt = '''                        elif text.startswith("Сброс "):
                            email_to_reset = text[6:].strip()
                            with get_aether_db() as conn:
                                cur = conn.cursor()
                                cur.execute("DELETE FROM trial_claims WHERE LOWER(user_identifier) = LOWER(?)", (email_to_reset,))
                                deleted = cur.rowcount
                                conn.commit()
                            await tg_send_message(chat_id, f"✅ Ограничение на триал снято с `{email_to_reset}` (удалено {deleted} записей).")
                            continue

                        elif text.lower().startswith("блокировка "):
                            email_to_block = text[11:].strip().lower()
                            with get_aether_db() as conn:
                                conn.execute("INSERT OR IGNORE INTO trial_claims(user_identifier, claimed_at) VALUES(?, ?)", (email_to_block, int(time.time())))
                                conn.commit()
                            await tg_send_message(chat_id, f"✅ Триал успешно заблокирован для `{email_to_block}`.")
                            continue'''

if old_txt in code:
    code = code.replace(old_txt, new_txt)

with open("/opt/aether-support/server.py", "w", encoding="utf-8") as f:
    f.write(code)

print("Support server trial features patched!")
