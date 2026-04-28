import getopt
import sys
import ssl
from flask import Flask, request
from models import User, Account
from database import db_session
import simplejson as json
from cheroot.wsgi import Server
from pathlib import Path

app = Flask(__name__)
DEFAULT_PORT_NO = 8888

# TLS/SSL Configuration
SSL_CERT_PATH = "path/to/your/certificate.crt"  # Your server certificate
SSL_KEY_PATH = "path/to/your/private_key.key"   # Your private key

def usageguide():
    print("InsecureBankv2 Backend-Server with TLS 1.3")
    print("Options: ")
    print("  --port p      serve on port p (default 8888)")
    print("  --cert file   path to SSL certificate")
    print("  --key file    path to SSL private key")
    print("  --help        print this message")

@app.errorhandler(500)
def internal_servererror(error):
    print("[!] Error:", error)
    return "Internal Server Error", 500

@app.route('/login', methods=['POST'])
def login():
    Responsemsg = "fail"
    user = request.form.get('username')
    password = request.form.get('password')
    
    u = User.query.filter(User.username == user).first()
    
    if u and u.password == password:
        Responsemsg = "Correct Credentials"
    elif u and u.password != password:
        Responsemsg = "Wrong Password"
    elif not u:
        Responsemsg = "User Does not Exist"
    else:
        Responsemsg = "Some Error"
    
    data = {"message": Responsemsg, "user": user}
    print("✓ Login request processed via TLS 1.3")
    return json.dumps(data)

@app.route('/getaccounts', methods=['POST'])
def getaccounts():
    Responsemsg = "fail"
    user = request.form.get('username')
    password = request.form.get('password')
    
    u = User.query.filter(User.username == user).first()
    
    if not u or u.password != password:
        Responsemsg = "Wrong Credentials so trx fail"
    else:
        Responsemsg = "Correct Credentials so get accounts will continue"
    
    from_acc = 0
    to_acc = 0
    
    if Responsemsg == "Correct Credentials so get accounts will continue":
        accounts = Account.query.filter(Account.user == user).all()
        for account in accounts:
            if account.type == 'from':
                from_acc = account.account_number
            elif account.type == 'to':
                to_acc = account.account_number
    
    data = {"message": Responsemsg, "from": from_acc, "to": to_acc}
    return json.dumps(data)

@app.route('/dotransfer', methods=['POST'])
def dotransfer():
    Responsemsg = "fail"
    user = request.form.get('username')
    password = request.form.get('password')
    
    u = User.query.filter(User.username == user).first()
    
    if not u or u.password != password:
        Responsemsg = "Wrong Credentials so trx fail"
    else:
        Responsemsg = "Success"
        from_acc = request.form.get('from_acc')
        to_acc = request.form.get('to_acc')
        amount = int(request.form.get('amount'))
        
        from_account = Account.query.filter(Account.account_number == from_acc).first()
        to_account = Account.query.filter(Account.account_number == to_acc).first()
        
        to_account.balance += amount
        from_account.balance -= amount
        db_session.commit()
    
    from_acc = request.form.get('from_acc')
    to_acc = request.form.get('to_acc')
    amount = request.form.get('amount')
    
    data = {"message": Responsemsg, "from": from_acc, "to": to_acc, "amount": amount}
    print("✓ Transfer request processed via TLS 1.3")
    return json.dumps(data)

@app.route('/changepassword', methods=['POST'])
def changepassword():
    Responsemsg = "fail"
    user = request.form.get('username')
    newpassword = request.form.get('newpassword')
    
    u = User.query.filter(User.username == user).first()
    
    if not u:
        Responsemsg = "Error"
    else:
        Responsemsg = "Change Password Successful"
        u.password = newpassword
        db_session.commit()
    
    data = {"message": Responsemsg}
    return json.dumps(data)

if __name__ == '__main__':
    port = DEFAULT_PORT_NO
    cert_file = SSL_CERT_PATH
    key_file = SSL_KEY_PATH
    
    options, args = getopt.getopt(sys.argv[1:], "", ["help", "port=", "cert=", "key="])
    
    for op, arg1 in options:
        if op == "--help":
            usageguide()
            sys.exit(2)
        elif op == "--port":
            port = int(arg1)
        elif op == "--cert":
            cert_file = arg1
        elif op == "--key":
            key_file = arg1
    
    print("The server is hosted on port:", port)
    print("TLS 1.3 enabled with certificate:", cert_file)
    
    try:
        # Create SSL context for TLS 1.3
        ssl_context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
        ssl_context.load_cert_chain(cert_file, key_file)
        ssl_context.minimum_version = ssl.TLSVersion.TLSv1_3
        ssl_context.maximum_version = ssl.TLSVersion.TLSv1_3
        
        # Start secure server
        server = Server(("0.0.0.0", port), app, ssl_context=ssl_context)
        print("✓ HTTPS server started with TLS 1.3")
        server.start()
    except KeyboardInterrupt:
        print("\nServer stopped")
    except Exception as e:
        print("✗ Failed to start server:", str(e))
