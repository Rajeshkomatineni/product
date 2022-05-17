from flask_sqlalchemy import SQLAlchemy
 
db =SQLAlchemy()
 
class ProductModel(db.Model):
    __tablename__ = "table"
 
    id = db.Column(db.Integer, primary_key=True)
    product_id = db.Column(db.Integer(),unique = True)
    product_name = db.Column(db.String())
    currency = db.Column(db.String())
    price = db.Column(db.Integer())
 
    def __init__(self, product_id,product_name,currency,price):
        self.product_id = product_id
        self.product_name = product_name
        self.currency = currency
        self.price = price
 
    def __repr__(self):
        return f"{self.product_name}:{self.product_id}"