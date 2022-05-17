from flask import Flask,render_template,request,redirect
from models import db,ProductModel
 
app = Flask(__name__)
 
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///data.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db.init_app(app)
 
@app.before_first_request
def create_table():
    db.create_all()
    db.session.commit()
 
@app.route('/data/create' , methods = ['GET','POST'])
def create():
    if request.method == 'GET':
        return render_template('./createpage.html')
 
    if request.method == 'POST':
        product_id = request.form['product_id']
        product_name = request.form['product_name']
        price = request.form['price']
        currency = request.form['currency']
        product = ProductModel(product_id=product_id, product_name=product_name, price=price, currency = currency)
        db.session.add(product)
        db.session.commit()
        return redirect('/data')
 
 
@app.route('/data')
def RetrieveList():
    products = ProductModel.query.all()
    return render_template('datalist.html',products = products)
 
 
@app.route('/data/<int:id>')
def RetrieveEmployee(id):
    product = ProductModel.query.filter_by(product_id=id).first()
    if product:
        return render_template('data.html', product = product)
    return f"Product with id ={id} Doenst exist"
 
 
@app.route('/data/<int:id>/update',methods = ['GET','POST'])
def update(id):
    Product = ProductModel.query.filter_by(product_id=id).first()
    if request.method == 'POST':
        if Product:
            db.session.delete(Product)
            print("1")
            db.session.commit()
            print("2")
            product_id = request.form['product_id']
            product_name = request.form['product_name']
            price = request.form['price']
            currency = request.form['currency']
            Product = ProductModel(product_id=product_id, product_name=product_name, price=price, currency = currency)
            print("hello")
            db.session.add(Product)
            db.session.commit()
            print("Hai")
            return redirect('/data/id')
        return f"Product with id = {id} Does not exist"
 
    return render_template('update.html', Product = Product)
 
 
@app.route('/data/<int:id>/delete', methods=['GET','POST'])
def delete(id):
    Product = ProductModel.query.filter_by( product_id=id).first()
    if request.method == 'POST':
        if Product:
            db.session.delete(Product)
            db.session.commit()
            return redirect('/data')
        #abort(404)
 
    return render_template('delete.html')
 
app.run(host='localhost', port=5000)