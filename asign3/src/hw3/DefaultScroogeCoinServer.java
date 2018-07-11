package hw3;

import java.security.*;
import java.util.*;

import hw3.Transaction.Input;
import hw3.Transaction.Output;
import hw3.Transaction.Type;

//Scrooge creates coins by adding outputs to a transaction to his public key.
//In ScroogeCoin, Scrooge can create as many coins as he wants.
//No one else can create a coin.
//A user owns a coin if a coin is transfer to him from its current owner
public class DefaultScroogeCoinServer implements ScroogeCoinServer {

	private KeyPair scroogeKeyPair;
	private ArrayList<Transaction> ledger = new ArrayList();

	//Set scrooge's key pair
	@Override
	public synchronized void init(KeyPair scrooge) {
		scroogeKeyPair=new KeyPair(scrooge.getPublic(),scrooge.getPrivate());
	}


	//For every 10 minute epoch, this method is called with an unordered list of proposed transactions
	// 		submitted during this epoch.
	//This method goes through the list, checking each transaction for correctness, and accepts as
	// 		many transactions as it can in a "best-effort" manner, but it does not necessarily return
	// 		the maximum number possible.
	//If the method does not accept an valid transaction, the user must try to submit the transaction
	// 		again during the next epoch.
	//Returns a list of hash pointers to transactions accepted for this epoch

	public synchronized List<HashPointer> epochHandler(List<Transaction> txs)  {
		List<HashPointer> hashList = new ArrayList<HashPointer>();
		while(!txs.isEmpty()){                
			List<Transaction> invalid = new ArrayList<Transaction>();
			for(Transaction t:txs){
				if(!isValid(t)){
					invalid.add(t);
				}else {	
					ledger.add(t);
					hashList.add(new HashPointer(t.getHash(),ledger.size()-1));
				}
			}
			if(txs.size()==invalid.size()) break;

			txs = invalid;
		}
		return hashList; 
	}

	//Returns true if and only if transaction tx meets the following conditions:
	//CreateCoin transaction
	//	(1) no inputs
	//	(2) all outputs are given to Scrooge's public key
	//	(3) all of tx’s output values are positive
	//	(4) Scrooge's signature of the transaction is included

	//PayCoin transaction
	//	(1) all inputs claimed by tx are in the current unspent (i.e. in getUTOXs()),
	//	(2) the signatures on each input of tx are valid,
	//	(3) no UTXO is claimed multiple times by tx,
	//	(4) all of tx’s output values are positive, and
	//	(5) the sum of tx’s input values is equal to the sum of its output values;
	@Override
	public synchronized boolean isValid(Transaction tx) {

		switch(tx.getType()) {

		case Create:
			try {
				Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
				sig.initVerify(scroogeKeyPair.getPublic());
				sig.update(tx.getRawBytes());
				if (!sig.verify(tx.getSignature())) {
					return false;
				}

				if(tx.getInputs()==null) return false;

				for(Transaction.Output output : tx.getOutputs()) {
					if(output.getPublicKey()!=scroogeKeyPair.getPublic() || (output.getValue()<0)) {
						return false;
					}			
				}
				return true;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}

		case Pay: 

			try {

				double totalInput = 0;
				double totalOutput = 0;

				for(Transaction.Output output : tx.getOutputs()){
					if(output.getValue()<=0) return false;
					totalOutput+=output.getValue();
				}

				for(int i=0;i<tx.numInputs();i++){
					Transaction.Input in = tx.getInputs().get(i);	
					int currentIndex = getCurrentIndex(in.getIndexOfTxOutput(),in,in.getHashOfOutputTx(),getUTXOs());
					if(currentIndex==-1) return false; 

					Transaction.Output outOfIn = ledger.get(currentIndex).getOutput(in.getIndexOfTxOutput());
					Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
					sig.initVerify(outOfIn.getPublicKey());
					sig.update(tx.getRawDataToSign(i));
					if (!sig.verify(in.getSignature())) {
						return false;
					}

					totalInput+=outOfIn.getValue();
				}

				if((totalInput-totalOutput)<0.000001){ 
					return true;
				}else{
					return false;
				}

			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}

		}
		return true;
	}

	@Override
	public synchronized Set<UTXO> getUTXOs() {
		Set<UTXO> utxoSet = new HashSet<UTXO>();

		for(int i = 0;i<ledger.size();i++){
			Transaction transaction = ledger.get(i);
			switch(transaction.getType()){

			case Create:
				//just output on create
				for(Transaction.Output output : transaction.getOutputs())
				{
					UTXO Newutxo = new UTXO(new HashPointer(transaction.getHash(),i),transaction.getIndex(output));
					utxoSet.add(Newutxo);
				}
				break;

			case Pay:
				//for output
				for(Transaction.Output output: transaction.getOutputs())
				{
					HashPointer hp = new HashPointer(transaction.getHash(),i);
					UTXO outUtxo = new UTXO(hp,transaction.getIndex(output));
					utxoSet.add(outUtxo);
				}
				//for input
				for(int t=0;t<transaction.numInputs();t++)
				{
					Transaction.Input input = transaction.getInputs().get(t);
					HashPointer hp = new HashPointer(input.getHashOfOutputTx(), getCurrentIndex(input.getIndexOfTxOutput(),input,input.getHashOfOutputTx(),utxoSet));
					UTXO inUtxo = new UTXO(hp,input.getIndexOfTxOutput());
					utxoSet.remove(inUtxo);
				}

				break;
			}

		}
		return utxoSet;
	}

	//get the current index of the transaction
	private int getCurrentIndex(int outIndex, Transaction.Input in,byte[] outTransHash,Set<UTXO> utxoSet) {
		for(int i=0;i<ledger.size();i++){
			if(Arrays.equals(ledger.get(i).getHash(),outTransHash)){
				UTXO utxo = new UTXO(new HashPointer(in.getHashOfOutputTx(), i),outIndex);
				if(utxoSet.contains(utxo)) {
					return i;
				}
			}
		}
		return -1;
	}

}
